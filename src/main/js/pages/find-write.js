import {createTextBox, drawTextObjects, getAngle, getDistance, getEventCoordinates} from "./common-find_park.js";
import {CanvasInteractionManager} from "./canvas-interaction";

export function initFindWrite() {

    // 캔버스 설정
    const imageCanvas = document.getElementById("image-canvas");
    const paintCanvas = document.getElementById("paint-canvas");
    const objectCanvas = document.getElementById("object-canvas");
    const imageCtx = imageCanvas.getContext("2d");
    const paintCtx = paintCanvas.getContext("2d");
    const objectCtx = objectCanvas.getContext("2d");
    let rect;

    // 그리기 관련 상태 변수
    let isDrawing = false;
    let isDraggingObject = false;
    let isPinching = false;         // 핀치 줌 동작 중인지 확인하는 플래그.
    let initialPinchDistance = null;    // 두 손가락의 초기 거리를 저장할 변수.
    let pinchStartImageState = {};      // 핀치 시작 시점의 이미지 상태를 저장할 객체.
    let selectedColor = "#000000";
    let penThickness = 5;
    let lastX = 0;
    let lastY = 0;

    // 데이터 저장소
    let backgroundImage = null;         // 이미지 정보를 저장할 전용 변수.
    let objects = [];                  // 텍스트 객체만 저장하는 배열.
    let selectedObject = null;          // 이미지 & 텍스트 객체 공통 할당 가능.
    let lastSelectedTextObject = null;
    let dragOffsetX, dragOffsetY;            // 마우스와 객체 좌상단의 간격.

    // UI 요소
    const addImageBtn = document.getElementById("add-image-btn");
    const imageLoader = document.getElementById("image-loader");
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

    // getDistance 제거 위치


    function initializeCanvases() {
        const canvasContainer = document.getElementById("canvas-container");
        rect = canvasContainer.getBoundingClientRect();
        const scale = window.devicePixelRatio;

        [imageCanvas, paintCanvas, objectCanvas].forEach((canvas) => {
            canvas.style.width = `${rect.width}px`;     // 스케일링 하지 않은 물리적 크기.
            canvas.style.height = `${rect.height}px`;
            canvas.width = rect.width * scale;          // 스케일링 한 논리적 크기.
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

    // --- 객체 생성 함수 ---

    /**
     * @description backgroundImage 변수에 저장된 정보를 바탕으로 image-canvas에 이미지를 그립니다.
     */
    function drawImageObject() {

        if (!backgroundImage) {
            return;         // 이미지가 없으면 아무것도 하지 않음.
        }

        // 1. 이미지 캔버스를 깨끗하게 지웁니다.
        imageCtx.clearRect(0, 0, rect.width, rect.height);

        // 2. 캔버스 상태 저장 (이미지 제외 타 객체 저장(제거))
        imageCtx.save();

        // 3. 캔버스 변형 적용
        // 3-1. 이미지 중심으로 이동
        imageCtx.translate(backgroundImage.translateX, backgroundImage.translateY);
        // 3-2. 이미지 회전
        imageCtx.rotate(backgroundImage.rotation);
        // 3-3. 이미지 확대/축소
        imageCtx.scale(backgroundImage.scale, backgroundImage.scale);

        // 4. 이미지 그리기 (이미지가 가운데 정렬 되도록 설정 포함.)
        imageCtx.drawImage(backgroundImage.image, -backgroundImage.originalWidth / 2, // x(좌측 끝 좌표) = 기존값(캔버스 중앙 x좌표) - 그림 너비의 절반값
            -backgroundImage.originalHeight / 2, backgroundImage.originalWidth, backgroundImage.originalHeight,)
        // 크기가 넘칠 경우, 캔버스 크기에 알맞게 조절하는 로직은 아래 loadImage 메소드에서 담당.

        // 5. 캔버스 상태 복원 (이미지 제외 타 객체 상태 복원)
        imageCtx.restore();

    }


    /**
     * @description 사용자가 선택한 이미지 파일을 읽어 캔버스에 추가하는 함수
     * @param {Event} event
     */
    function loadImage(event) {

        const file = event.target.files[0]; // 선택한 파일들 중 첫 번째 파일.
        if (!file) return;

        const reader = new FileReader();

        reader.onload = function (event) {

            // 이미지 데이터를 담을 Image 객체 생성.
            const image = new Image();

            // 이미지 로딩이 완료되었을 때 실행될 콜백 함수.
            image.onload = function () {

                // 이미지가 캔버스보다 크면 비율에 맞게 축소하기 위한 로직.
                const maxWidth = rect.width;
                const maxHeight = rect.height;
                let width = image.width;
                let height = image.height;

                if (width > maxWidth || height > maxHeight) {
                    // 1. 너비를 맞추기 위한 축소 비율 계산
                    const widthRatio = maxWidth / width;
                    // 2. 높이를 맞추기 위한 축소 비율 계산
                    const heightRatio = maxHeight / height;
                    // 3. 두 비율 중 더 작은 값(더 많이 축소해야 하는 값)을 최종 비율로 선택.
                    const ratio = Math.min(widthRatio, heightRatio);

                    // 4. 너비와 높이에 동일한 비율을 적용하여 크기 재계산
                    width = width * ratio;
                    height = height * ratio;

                }

                // 1. 이미지 정보를 전역 변수 backgroundImage에 객체로 저장합니다.
                backgroundImage = {
                    type: 'image', image: image, originalWidth: width, originalHeight: height, // 이미지 삽입 시 가운데 정렬하기 위한 x, y 좌표 계산.
                    // translateX는 이미지의 '중심점'이 캔버스에서 위치하는 'x좌표'.
                    translateX: rect.width / 2,   // 캔버스 중앙 x좌표 (가운데 정렬은 imageCtx.drawImage()에서 처리)
                    translateY: rect.height / 2, // width: width,   // 최종 조절된 너비.
                    // height: height, // 최종 조절된 높이.
                    scale: 1,           // 현재 확대/축소 비율.
                    rotation: 0         // 현재 회전 각도(라디안).
                };

                drawImageObject();

            }

            // FileReader가 읽은 파일 데이터(Data URL)를 Image 객체의 소스로 지정.
            // 이 코드가 실행되어야 위 image.onload가 트리거됨.
            image.src = event.target.result;

        }

        // 선택된 파일을 Data URL 형식으로 읽기 시작 (비동기).
        reader.readAsDataURL(file);

        // input의 값을 초기화하여 동일한 파일을 다시 선택해도 change 이벤트가 발생하도록 함.
        event.target.value = null;

    }

    const interactionConfig = {
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

            // 2. 배경 이미지 탐색
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

            // 3. 아무것도 선택되지 않으면 null 반환 (-> 그리기 모드 시작)
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
            // manager 내부의 selectedObject를 참조하여 타입에 맞게 다시 그림
            const selectedObject = interactionManager.selectedObject;
            if (selectedObject.type === 'image') {
                drawImageObject();
            } else if (selectedObject.type === 'text') {
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
                updated = true; // 스케일이 강제로 변경되었음을 알림
            }
            fontSizeSlider.value = visualSize;
            fontSizeValue.textContent = visualSize;
            return updated;
        },
        onInteractionEnd: () => {
            // 이 파일에서는 특별히 할 작업 없음
        }
    };

    // --- 이벤트 리스너 등록 ---
    // 색상 변경
    colorButtons.forEach((button) => {
        button.addEventListener("click", () => {

            // 1. 새 색상 가져오기 및 공통 변수 업데이트
            const newColor = window.getComputedStyle(button).backgroundColor;
            selectedColor = newColor;

            // 2. 마지막으로 선택한 텍스트 객체가 있으면 색상 적용 및 다시 그리기
            if (lastSelectedTextObject) {

                lastSelectedTextObject.color = newColor;
                drawTextObjects(objects, objectCtx, rect);

            }
        });
    })

    // 굵기 변경
    thicknessSlider.addEventListener("input", (event) => {
        penThickness = event.target.value;
    });

    // 글자 크기 변경
    fontSizeSlider.addEventListener("input", (event) => {

        const newSize = event.target.value;
        fontSizeValue.textContent = newSize;

        if (textInput.value !== "") {
            return;
        }

        if (lastSelectedTextObject) {
            lastSelectedTextObject.scale = newSize / lastSelectedTextObject.fontSize;
            drawTextObjects(objects, objectCtx, rect);
        }
    })

    addTextBtn.addEventListener("click", () => {
        createTextBox(objects, obj => lastSelectedTextObject = obj, textInput, fontSizeContainer, fontSizeSlider, drawTextObjects, selectedColor, objectCtx, rect);
    });
    addImageBtn.addEventListener("click", () => imageLoader.click());
    imageLoader.addEventListener("change", loadImage);

    // --- 초기 실행 ---
    initializeCanvases();
    const interactionManager = new CanvasInteractionManager(objectCanvas, objects, interactionConfig);
    interactionManager.registerEvents();

}