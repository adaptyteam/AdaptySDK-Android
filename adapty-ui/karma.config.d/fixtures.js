// Serves the calm-onboarding fixture and wasmJsTest/resources to wasm browser tests.
// Karma serves files outside basePath under `/absolute<abs path>`; tests read those URLs from client config.
// adaptyUi*Dir() come from the Gradle-generated paths.js: both files are concatenated into one
// function body (sorted by name), so the hoisted function declarations are visible here.
// Without paths.js (e.g. an IDE run outside Gradle) nothing is registered and tests fail with a clear message.
(function () {
    config.client = config.client || {};
    if (typeof adaptyUiFixturesDir === 'function') {
        var dir = adaptyUiFixturesDir();
        config.files.push({ pattern: dir + "/**/*", served: true, included: false, watched: false });
        config.client.adaptyUiFixturesUrl = "/absolute" + dir;
    }
    if (typeof adaptyUiTestResourcesDir === 'function') {
        var res = adaptyUiTestResourcesDir();
        config.files.push({ pattern: res + "/**/*", served: true, included: false, watched: false });
        config.client.adaptyUiTestResourcesUrl = "/absolute" + res;
    }
})();
