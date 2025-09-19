import {createTextBox, drawTextObjects} from "./common-find_park.js";
import {CanvasInteractionManager} from "./canvas-interaction";

/**
 * 이미지 기능을 제외한 캔버스의 핵심 기능(그리기, 텍스트 추가/수정)을 초기화합니다.
 * @returns {{interactionManager: CanvasInteractionManager, imageCtx: CanvasRenderingContext2D, rect: DOMRect}}
 * 이미지 기능 등 추가적인 기능을 설정하는 데 필요한 객체들을 반환합니다.
 */
export function setupTextAndDrawControls() {

    // 캔버스 설정
    const imageCanvas = document.getElementById("image-canvas");
    const paintCanvas = document.getElementById("paint-canvas");
    const objectCanvas = document.getElementById("object-canvas");
    const imageCtx = imageCanvas.getContext("2d");
    const paintCtx = paintCanvas.getContext("2d");
    const objectCtx = objectCanvas.getContext("2d");
    let rect;

    // 그리기 관련 상태 변수
    let selectedColor = "#000000";
    let penThickness = 5;

    // 데이터 저장소
    let objects = [];                  // 텍스트 객체만 저장하는 배열.
    let lastSelectedTextObject = null;

    // UI 요소
    const colorButtons = document.querySelectorAll("#container-color > div");
    const thicknessSlider = document.getElementById("thickness");
    const textInput = document.getElementById("text-input");
    const addTextBtn = document.getElementById("add-text-btn");
    const fontSizeSlider = document.getElementById("font-size-slider");
    const fontSizeValue = document.getElementById("font-size-value");
    const fontSizeContainer = document.getElementById("container-font-size");

    if (!paintCanvas.getContext || !objectCanvas.getContext || !imageCanvas.getContext) {
        alert("canvas context 불러오기 실패");
        return;
    }

    function initializeCanvases() {
        const canvasContainer = document.getElementById("canvas-container");
        rect = canvasContainer.getBoundingClientRect();
        const scale = window.devicePixelRatio;

        [imageCanvas, paintCanvas, objectCanvas].forEach((canvas) => {
            canvas.style.width = `${rect.width}px`;
            canvas.style.height = `${rect.height}px`;
            canvas.width = rect.width * scale;
            canvas.height = rect.height * scale;
        })

        thicknessSlider.min = 1;
        thicknessSlider.max = 50;
        thicknessSlider.value = penThickness;

        fontSizeSlider.max = 200;
        fontSizeContainer.style.display = "none";

        imageCtx.scale(scale, scale);
        paintCtx.scale(scale, scale);
        objectCtx.scale(scale, scale);
        paintCtx.lineCap = "round";
    }

    const interactionConfig = {

        isInteractionDisabled: () => textInput.value !== "",

        findSelectableObject: (coord) => {
            if (textInput.value !== "") return null;

            // 1. 텍스트 객체 우선 탐색 (역변환 히트 테스트)
            for (let i = objects.length - 1; i >= 0; i--) {
                const object = objects[i];
                const relX = coord.x - object.translateX;
                const relY = coord.y - object.translateY;
                const angle = -object.rotation;
                const rotatedX = relX * Math.cos(angle) - relY * Math.sin(angle);
                const rotatedY = relX * Math.sin(angle) + relY * Math.cos(angle);
                const transformedX = rotatedX / object.scale;
                const transformedY = rotatedY / object.scale;
                const textHeight = object.ascent + object.descent;
                if (Math.abs(transformedX) <= object.width / 2 && Math.abs(transformedY) <= textHeight / 2) {
                    return object;
                }
            }

            // 2. 아무것도 선택되지 않으면 null 반환 (-> 그리기 모드 시작)
            return null;
        },
        onObjectSelect: (selectedObject) => {
            if (selectedObject.type === 'text') {
                lastSelectedTextObject = selectedObject;
                const visualSize = Math.round(selectedObject.fontSize * selectedObject.scale);
                fontSizeSlider.value = visualSize;
                fontSizeValue.textContent = visualSize;
                fontSizeContainer.style.display = "block";
            } else {
                lastSelectedTextObject = null;
                fontSizeContainer.style.display = "none";
            }
        },
        onObjectMove: () => {
            const selectedObject = interactionManager.selectedObject;
            if (selectedObject.type === 'text') {
                drawTextObjects(objects, objectCtx, rect);
            }
        },
        onDrawStart: () => {
            fontSizeContainer.style.display = "none";
        },
        onDrawMove: (from, to) => {
            paintCtx.strokeStyle = selectedColor;
            paintCtx.lineWidth = penThickness;
            paintCtx.beginPath();
            paintCtx.moveTo(from.x, from.y);
            paintCtx.lineTo(to.x, to.y);
            paintCtx.stroke();
        },
        onPinchUpdate: (selectedObject) => {
            if (selectedObject.type !== 'text') return false;

            let visualSize = Math.round(selectedObject.fontSize * selectedObject.scale);
            const maxSize = 200;
            let updated = false;
            if (visualSize > maxSize) {
                selectedObject.scale = maxSize / selectedObject.fontSize;
                visualSize = maxSize;
                updated = true;
            }
            fontSizeSlider.value = visualSize;
            fontSizeValue.textContent = visualSize;
            return updated;
        },
        onInteractionEnd: () => {}
    };

    // --- 이벤트 리스너 등록 ---
    colorButtons.forEach((button) => {
        button.addEventListener("click", () => {
            const newColor = window.getComputedStyle(button).backgroundColor;
            selectedColor = newColor;
            if (textInput.value !== "") return;
            if (lastSelectedTextObject) {
                lastSelectedTextObject.color = newColor;
                drawTextObjects(objects, objectCtx, rect);
            }
        });
    })

    thicknessSlider.addEventListener("input", (event) => {
        penThickness = event.target.value;
    });

    fontSizeSlider.addEventListener("input", (event) => {
        const newSize = event.target.value;
        fontSizeValue.textContent = newSize;
        if (textInput.value !== "" || !lastSelectedTextObject) return;
        lastSelectedTextObject.scale = newSize / lastSelectedTextObject.fontSize;
        drawTextObjects(objects, objectCtx, rect);
    })

    textInput.addEventListener("input", () => {
        fontSizeContainer.style.display = "block";
        lastSelectedTextObject = null;
    });
    addTextBtn.addEventListener("click", () => {
        createTextBox(objects, obj => lastSelectedTextObject = obj, textInput, fontSizeContainer, fontSizeSlider, drawTextObjects, selectedColor, objectCtx, rect);
    });

    // --- 초기 실행 ---
    initializeCanvases();
    const interactionManager = new CanvasInteractionManager(objectCanvas, objects, interactionConfig);
    interactionManager.registerEvents();

    // 다른 모듈에서 이미지 기능을 추가할 수 있도록 주요 객체들을 반환합니다.
    return { interactionManager, imageCtx, rect };
}

/**
 * initFindWrite로 생성된 캔버스에 이미지 추가 및 제어 기능을 설정합니다.
 * @param {{interactionManager: CanvasInteractionManager, imageCtx: CanvasRenderingContext2D, rect: DOMRect}} deps
 * - initFindWrite에서 반환된 의존성 객체.
 */
export function setupImageControls({ interactionManager, imageCtx, rect }) {
    let backgroundImage = null;
    const addImageBtn = document.getElementById("add-image-btn");
    const imageLoader = document.getElementById("image-loader");

    function drawImageObject() {
        if (!backgroundImage) return;
        imageCtx.clearRect(0, 0, rect.width, rect.height);
        imageCtx.save();
        imageCtx.translate(backgroundImage.translateX, backgroundImage.translateY);
        imageCtx.rotate(backgroundImage.rotation);
        imageCtx.scale(backgroundImage.scale, backgroundImage.scale);
        imageCtx.drawImage(backgroundImage.image, -backgroundImage.originalWidth / 2, -backgroundImage.originalHeight / 2, backgroundImage.originalWidth, backgroundImage.originalHeight);
        imageCtx.restore();
    }

    function loadImage(event) {
        const file = event.target.files[0];
        if (!file) return;
        const reader = new FileReader();
        reader.onload = (e) => {
            const image = new Image();
            image.onload = () => {
                const maxWidth = rect.width;
                const maxHeight = rect.height;
                let { width, height } = image;
                if (width > maxWidth || height > maxHeight) {
                    const ratio = Math.min(maxWidth / width, maxHeight / height);
                    width *= ratio;
                    height *= ratio;
                }
                backgroundImage = {
                    type: 'image', image: image, originalWidth: width, originalHeight: height,
                    translateX: rect.width / 2, translateY: rect.height / 2,
                    scale: 1, rotation: 0
                };
                drawImageObject();
            };
            image.src = e.target.result;
        };
        reader.readAsDataURL(file);
        event.target.value = null;
    }

    // 기존 상호작용 설정에 이미지 관련 로직을 '추가'합니다.
    const originalFindSelectableObject = interactionManager.config.findSelectableObject;
    interactionManager.config.findSelectableObject = (coord) => {
        // 기존 로직(텍스트 객체 찾기)을 먼저 실행합니다.
        const foundObject = originalFindSelectableObject(coord);
        if (foundObject) return foundObject;

        // 텍스트 객체가 없으면 배경 이미지를 탐색합니다.
        if (backgroundImage) {
            const relX = coord.x - backgroundImage.translateX;
            const relY = coord.y - backgroundImage.translateY;
            const angle = -backgroundImage.rotation;
            const rotatedX = relX * Math.cos(angle) - relY * Math.sin(angle);
            const rotatedY = relX * Math.sin(angle) + relY * Math.cos(angle);
            const transformedX = rotatedX / backgroundImage.scale;
            const transformedY = rotatedY / backgroundImage.scale;
            if (Math.abs(transformedX) <= backgroundImage.originalWidth / 2 && Math.abs(transformedY) <= backgroundImage.originalHeight / 2) {
                return backgroundImage;
            }
        }
        return null;
    };

    const originalOnObjectMove = interactionManager.config.onObjectMove;
    interactionManager.config.onObjectMove = () => {
        const selectedObject = interactionManager.selectedObject;
        if (selectedObject && selectedObject.type === 'image') {
            drawImageObject();
        } else {
            // 이미지가 아니면 기존 이동 로직(텍스트)을 실행합니다.
            originalOnObjectMove();
        }
    };

    addImageBtn.addEventListener("click", () => imageLoader.click());
    imageLoader.addEventListener("change", loadImage);
}

export function initFindWrite() {
    // --- 페이지 로드 시 최종 실행 ---
    const dependencies = setupTextAndDrawControls();
    setupImageControls(dependencies);
}