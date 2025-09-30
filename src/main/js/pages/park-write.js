// 로컬 테스트용
// import {setAndDrawBackgroundImage, setupTextAndDrawControls} from "../find-in-progress/find-write.js";
// 실제 서비스용
import {setAndDrawBackgroundImage, setupTextAndDrawControls} from "./find-write.js";

async function setInitialBackgroundImage({imageCtx, rect, updateSaveBtnState, scale}) {
    const parkUrl = document.body.getAttribute('data-park-url');

    try {
        // 👇 loadImageFromS3가 Image 객체를 반환
        const img = await loadImageFromS3(parkUrl);
        setAndDrawBackgroundImage(img, imageCtx, rect, scale);
        updateSaveBtnState();
    } catch (error) {
        console.error("배경 이미지 로딩 실패:", error);
        alert(`박명록을 작성해보세요`);
        // alert("이미지를 불러오는 데 실패했습니다.");
    }
}

async function loadImageFromS3(imageUrl) {
    const imageResponse = await fetch(imageUrl);
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
