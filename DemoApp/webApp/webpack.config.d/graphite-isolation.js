config.devServer = config.devServer || {};
config.devServer.headers = {
    ...(config.devServer.headers || {}),
    "Cross-Origin-Opener-Policy": "same-origin",
    "Cross-Origin-Embedder-Policy": "require-corp",
};
