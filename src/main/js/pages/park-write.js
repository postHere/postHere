// 로컬 테스트용
// import {setAndDrawBackgroundImage, setupTextAndDrawControls} from "../find-in-progress/find-write.js";
// 실제 서비스용
import {setAndDrawBackgroundImage, setupTextAndDrawControls} from "./find-write.js";

async function setInitialBackgroundImage({imageCtx, rect, updateSaveBtnState, scale}) {
    const parkUrl = document.body.getAttribute('data-park-url');
    console.log(`Found ${parkUrl}`);
    if (!parkUrl) return;

    try {
        // 👇 loadImageFromS3가 Image 객체를 반환
        const img = await loadImageFromS3(parkUrl);
        setAndDrawBackgroundImage(img, imageCtx, rect, scale);
        updateSaveBtnState();
    } catch (error) {
        console.error("배경 이미지 로딩 실패:", error);
        // alert("이미지를 불러오는 데 실패했습니다.");
    }
}

async function loadImageFromS3(imageUrl) {
    const response = await fetch(imageUrl);
    if (!response.ok) {
        throw new Error(`이미지 로드 실패: ${response.statusText}`);
    }
    const blob = await response.blob();
    return new Promise((resolve, reject) => {
        const img = new Image();
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
