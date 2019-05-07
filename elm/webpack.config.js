module.exports = [{
  entry: ['./app.scss', './elm-mdc/src/elm-mdc.js'],
  output: {
    filename: 'www/elm-mdc.js',
  },
  module: {
    rules: [
      {
        test: /\.scss$/,
        use: [
          {
            loader: 'file-loader',
            options: {
              name: 'www/app.css',
            },
          },
          { loader: 'extract-loader' },
          { loader: 'css-loader' },
          { loader: 'sass-loader',
            options: {
              includePaths: ['./node_modules']
            }
          },
        ]
      },
      {
        test: /\.js$/,
        loader: 'babel-loader',
        query: { presets: ['es2015'] }
      },
    ]
  },
}];