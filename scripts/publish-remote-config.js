/*
 * Publishes release metadata to Firebase Remote Config so the in-app updater
 * can check for updates without hitting the GitHub API from every device.
 *
 * Required environment variables:
 *   FIREBASE_SERVICE_ACCOUNT  service-account JSON (GitHub Actions secret)
 *   GITHUB_TOKEN              token with read access to the release
 *   REPOSITORY                owner/repo
 *   RELEASE_TAG               tag that was published, e.g. v1.1
 */

const admin = require("firebase-admin");

const { FIREBASE_SERVICE_ACCOUNT, GITHUB_TOKEN, REPOSITORY, RELEASE_TAG } = process.env;

function fail(message) {
  console.error(message);
  process.exit(1);
}

function parseVersion(value) {
  const core = String(value || "")
    .trim()
    .replace(/^v/i, "")
    .split("-")[0]
    .split("+")[0];
  if (!core) return null;
  const parts = core.split(".").map((part) => Number(part));
  if (parts.some((part) => !Number.isInteger(part) || part < 0)) return null;
  return parts;
}

function isNewer(candidate, current) {
  const a = parseVersion(candidate);
  const b = parseVersion(current);
  if (!a || !b) return true;
  const size = Math.max(a.length, b.length);
  for (let index = 0; index < size; index += 1) {
    const left = a[index] ?? 0;
    const right = b[index] ?? 0;
    if (left !== right) return left > right;
  }
  return false;
}

async function main() {
  if (!FIREBASE_SERVICE_ACCOUNT) fail("FIREBASE_SERVICE_ACCOUNT secret is missing");
  if (!GITHUB_TOKEN) fail("GITHUB_TOKEN is missing");
  if (!REPOSITORY || !RELEASE_TAG) fail("REPOSITORY and RELEASE_TAG are required");

  const response = await fetch(
    `https://api.github.com/repos/${REPOSITORY}/releases/tags/${encodeURIComponent(RELEASE_TAG)}`,
    {
      headers: {
        Accept: "application/vnd.github+json",
        Authorization: `Bearer ${GITHUB_TOKEN}`,
        "User-Agent": "BarbellBase-release-bot",
        "X-GitHub-Api-Version": "2022-11-28",
      },
    },
  );

  if (!response.ok) {
    fail(`GitHub release lookup failed with HTTP ${response.status}`);
  }

  const release = await response.json();
  const asset = (release.assets || []).find((item) => item.name.toLowerCase().endsWith(".apk"));
  if (!asset) fail("No APK asset was found on the release");
  if (!asset.digest) fail("GitHub did not return a digest for the APK asset");

  const version = RELEASE_TAG.replace(/^v/i, "");
  const sha256 = asset.digest.replace(/^sha256:/i, "");

  admin.initializeApp({
    credential: admin.credential.cert(JSON.parse(FIREBASE_SERVICE_ACCOUNT)),
  });

  const remoteConfig = admin.remoteConfig();
  const template = await remoteConfig.getTemplate();
  const parameters = template.parameters || {};

  const current = parameters.latest_version?.defaultValue?.value;
  if (current && !isNewer(version, current)) {
    console.log(`Skipping ${version}: Remote Config already points at ${current}.`);
    return;
  }

  const set = (key, value) => {
    parameters[key] = { defaultValue: { value: String(value) } };
  };

  set("latest_version", version);
  set("latest_tag", RELEASE_TAG);
  set("apk_url", asset.browser_download_url);
  set("apk_sha256", sha256);
  set("apk_size_bytes", asset.size);
  set("release_notes", (release.body || "").slice(0, 1800));

  template.parameters = parameters;
  await remoteConfig.publishTemplate(template);
  console.log(`Published Remote Config for ${RELEASE_TAG} (${version}).`);
}

main().catch((error) => fail(error.stack || String(error)));
