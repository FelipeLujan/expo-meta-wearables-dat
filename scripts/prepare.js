/**
 * Cross-platform prepare script.
 *
 * - Git clones (npm/yarn/pnpm git deps): no .git in cache → skip husky
 * - Windows: avoid expo-module-prepare bash script; compile with tsc instead
 * - Dev clone: run husky when .git exists; build if build/ is missing
 *
 * Uses process.cwd() (npm sets cwd to this package root for lifecycle scripts)
 * so we avoid __dirname, which is undefined when Node loads this file as ESM.
 */
const { execSync } = require("child_process");
const { existsSync, readFileSync } = require("fs");
const path = require("path");

const root = process.cwd();
const pkgPath = path.join(root, "package.json");

if (!existsSync(pkgPath)) {
  throw new Error(
    "[expo-meta-wearables-dat] prepare: run from package root (package.json not found)."
  );
}

const pkg = JSON.parse(readFileSync(pkgPath, "utf8"));
if (pkg.name !== "expo-meta-wearables-dat") {
  throw new Error(
    `[expo-meta-wearables-dat] prepare: unexpected package "${pkg.name ?? "unknown"}".`
  );
}

const buildIndex = path.join(root, "build", "index.js");
const pluginBuildIndex = path.join(root, "plugin", "build", "index.js");

function run(cmd, options = {}) {
  execSync(cmd, { stdio: "inherit", cwd: root, ...options });
}

if (existsSync(path.join(root, ".git"))) {
  try {
    run("husky");
  } catch {
    // Non-fatal — e.g. CI or husky not on PATH
  }
}

if (!existsSync(buildIndex) || !existsSync(pluginBuildIndex)) {
  console.log("[expo-meta-wearables-dat] Compiling TypeScript (build/ + plugin/build/)...");
  run("npx tsc --project tsconfig.json");
  run("npx tsc --project plugin/tsconfig.json");
}
