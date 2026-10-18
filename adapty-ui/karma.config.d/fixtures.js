// Serves the calm-onboarding fixture to wasm browser tests.
// Karma serves files outside basePath under `/absolute<abs path>`; the test reads that URL from client config.
// adaptyUiFixturesDir() comes from the Gradle-generated paths.js: both files are concatenated into one
// function body (sorted by name), so the hoisted function declaration is visible here.
(function () {
    var dir = adaptyUiFixturesDir();
    config.files.push({ pattern: dir + "/**/*", served: true, included: false, watched: false });
    config.client = config.client || {};
    config.client.adaptyUiFixturesUrl = "/absolute" + dir;
})();
