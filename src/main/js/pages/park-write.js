// 로컬 테스트용
// import {setAndDrawBackgroundImage, setupTextAndDrawControls} from "../find-in-progress/find-write.js";
// 실제 서비스용
import {setAndDrawBackgroundImage, setupTextAndDrawControls} from "./find-write.js";

async function setInitialBackgroundImage({imageCtx, rect, updateSaveBtnState, scale}) {
    // const parkUrl = document.body.getAttribute('data-park-url');
    const nickname = document.body.getAttribute('data-nickname');
    if (!nickname) return;

    try {
        // 👇 loadImageFromS3가 Image 객체를 반환
        const img = await loadImageFromS3(`/profile/park/${nickname}`);
        alert(`await loadImageFromS3(/profile/park/${nickname} 결과: ${img}`);
        setAndDrawBackgroundImage(img, imageCtx, rect, scale);
        updateSaveBtnState();
    } catch (error) {
        console.error("배경 이미지 로딩 실패:", error);
        alert(`이미지 로딩 실패 원인: ${error.message}`);
        // alert("이미지를 불러오는 데 실패했습니다.");
    }
}

async function loadImageFromS3(apiUrl) {
    // 1. API 서버에 JSON 데이터 요청
    console.log(`API 요청: ${apiUrl}`);
    // const apiResponse = await fetch(apiUrl);
    const apiResponse = await fetch(apiUrl, {
        mode: 'cors'
    });

    if (!apiResponse.ok) {
        throw new Error(`API 응답 실패: ${apiResponse.status} ${apiResponse.statusText}`);
    }

    // 2. JSON 응답을 객체로 변환하고 이미지 URL 추출
    const data = await apiResponse.json();
    const imageUrl = data.contentCaptureUrl;
    // alert(imageUrl);

    if (!imageUrl) {
        throw new Error("JSON 응답에서 이미지 URL(contentCaptureUrl)을 찾을 수 없습니다.");
    }

    // 3. 추출한 URL로 실제 이미지 데이터 요청
    alert(`실제 이미지 로딩: ${imageUrl}`);
    const imageResponse = await fetch(imageUrl);
    alert(`await fetch(imageUrl) 결과: ${imageResponse}`);

    if (!imageResponse.ok) {
        throw new Error(`실제 이미지 로드 실패: ${imageResponse.status} ${imageResponse.statusText}`);
    }

    // 4. 이미지 데이터를 Blob으로 변환하여 Image 객체 생성
    const blob = await imageResponse.blob();
    return new Promise((resolve, reject) => {
        const img = new Image();

        // S3 같은 외부 도메인에서 이미지를 불러와 canvas에 사용할 때 발생할 수 있는
        // CORS(Cross-Origin Resource Sharing) 오류를 방지하기 위해 필요합니다.
        // img.crossOrigin = "Anonymous";

        img.onload = () => resolve(img);
        img.onerror = (err) => reject(err);
        img.src = URL.createObjectURL(blob);
    });
}

export function initParkWrite() {

    const dependencies = setupTextAndDrawControls();
    setInitialBackgroundImage(dependencies);
}

// 로컬 테스트용 코드
// initParkWrite();
