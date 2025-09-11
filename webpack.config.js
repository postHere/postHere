const path = require('path');

module.exports = {
    entry: './src/main/js/app.js', // ① JS 코드의 시작점
    output: {
        filename: 'bundle.js', // ② 합쳐진 파일의 이름
        path: path.resolve(__dirname, 'src/main/resources/static/js'), // ③ 결과물 저장 경로
    },
    module: {
        rules: [{
            test: /\.js$/,
            exclude: /node_modules/,
            use: {
                loader: 'babel-loader',
                presets: ['@babel/preset-env']
            }
        }]
    },
    mode: 'development'
};