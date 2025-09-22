import { setupTextAndDrawControls, setAndDrawBackgroundImage } from "./find-write.js";

async function setInitialBackgroundImage({ imageCtx, rect }) {
    const findUrl = document.body.getAttribute('data-find-url');
    if (!findUrl) return;

    try {
        // 👇 loadImageFromS3가 Image 객체를 반환하므로, 변수명을 img로 변경합니다.
        const img = await loadImageFromS3(findUrl);
        // 👇 새로 만든 설정 및 그리기 함수를 호출합니다.
        setAndDrawBackgroundImage(img, imageCtx, rect);
    } catch (error) {
        console.error("배경 이미지 로딩 실패:", error);
        alert("이미지를 불러오는 데 실패했습니다.");
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

export function initFindOverWrite() {

    const dependencies = setupTextAndDrawControls();
    // imageCtx와 rect를 넘겨줍니다.
    setInitialBackgroundImage(dependencies);
}

// 페이지의 진입점 역할을 하는 이 함수를 호출해야 합니다.
// 예: window.onload = initFindOverwrite; 또는 <script> 태그 마지막에서 호출
initFindOverWrite();