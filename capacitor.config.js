// 1. 방금 만든 서버 설정 파일을 불러옵니다.
const serverConfig = require('./SERVER_URL.js');

// 2. Capacitor의 기본 설정을 정의합니다.
const config = {
    appId: 'com.nokasegu.post_here',
    appName: 'post-here',
    webDir: 'src/main/resources/static',
    server: {
        // 3. 불러온 설정 파일의 url 값을 사용합니다.
        url: serverConfig.url,
        cleartext: true
    }
};

// 4. 최종 설정을 export 합니다.
module.exports = config;
