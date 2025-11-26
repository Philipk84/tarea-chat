const path = require("path");
const HtmlWebpackPlugin = require("html-webpack-plugin");

module.exports = {
  mode: "development",
  entry: "./index.js",
  output: {
    filename: "bundle.js",
    path: path.resolve(__dirname, "dist"),
    clean: true,
  },
  devtool: "inline-source-map",
  resolve: {
    fallback: {
      fs: false,
      net: false,
      tls: false,
    },
  },
  devServer: {
    static: {
      directory: path.join(__dirname, "dist"),
    },
    host: "0.0.0.0",
    port: 8080,
    open: true,
    hot: true,
    historyApiFallback: true,
    proxy: [
      {
        context: ["/api"],
        target: "http://localhost:3001",
        pathRewrite: { "^/api": "" },
        changeOrigin: true,
        secure: false,
      },
      {
        context: ["/voice"],
        target: "http://localhost:3001",
        changeOrigin: true,
        secure: false,
      },
    ],
  },
  module: {
    rules: [
      {
        test: /\.css$/i,
        use: ["style-loader", "css-loader"],
      },
    ],
  },
  plugins: [
    new HtmlWebpackPlugin({
      template: "./index.html",
      filename: "index.html",
    }),
  ],
  ignoreWarnings: [
    {
      module: /Services\.js$/,
      message: /Critical dependency: require function is used/,
    },
  ],
};
