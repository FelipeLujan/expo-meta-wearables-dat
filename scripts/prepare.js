/**
 * Cross-platform prepare script.
 *
 * - Git clones (npm/yarn/pnpm git deps): no .git in cache → skip husky
 * - Windows: avoid expo-module-prepare bash script; compile with tsc instead
 * - Dev clone: run husky when .git exists; build if build/ is missing
 */
const { existsSync } = require("fs");
const { execSync } = require("child_process");
const path = require("path");

const root = path.join(__dirname, "..");
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
