import {createTextBox, getAngle, getDistance, getEventCoordinates, drawTextObjects} from "./common-find_park.js";

export function initParkWrite() {
    // 캔버스 설정
    const objectCanvas = document.getElementById("object-canvas");
    const objectCtx = objectCanvas.getContext("2d");
    let rect;

    // 그리기 관련 상태 변수 (텍스트 객체 조작)
    let isDraggingObject = false;
    let isPinching = false;
    let initialPinchDistance = null;
    let pinchStartObjectState = {};
    let selectedColor = "#000000";

    // 데이터 저장소
    let objects = [];
    let selectedObject = null;
    let lastSelectedTextObject = null;
    let dragOffsetX, dragOffsetY;

    // UI 요소
    const colorButtons = document.querySelectorAll("#container-color > div");
    const textInput = document.getElementById("text-input");
    const addTextBtn = document.getElementById("add-text-btn");
    const fontSizeSlider = document.getElementById("font-size-slider");
    const fontSizeValue = document.getElementById("font-size-value");
    const fontSizeContainer = document.getElementById("container-font-size");
    const saveBtn = document.getElementById("save-btn");

    if (!objectCanvas.getContext) {
        alert("canvas context 불러오기 실패");
        return;
    }

    function initializeCanvases() {
        const canvasContainer = document.getElementById("canvas-container");
        rect = canvasContainer.getBoundingClientRect();
        const scale = window.devicePixelRatio;

        objectCanvas.style.width = `${rect.width}px`;
        objectCanvas.style.height = `${rect.height}px`;
        objectCanvas.width = rect.width * scale;
        objectCanvas.height = rect.height * scale;

        fontSizeSlider.max = 200;
        fontSizeContainer.style.display = "none";

        objectCtx.scale(scale, scale);
    }

    function startDragOrDrawing(event) {
        // 텍스트 입력란에 값이 있으면 텍스트 객체 조작을 비활성화합니다.
        if (textInput.value !== "") {
            return;
        }

        if (event.touches && event.touches.length === 2) {

            isPinching = true;
            isDraggingObject = true;
            const touch1 = event.touches[0];
            const touch2 = event.touches[1];
            initialPinchDistance = getDistance(touch1, touch2);
            const pinchCenterX = (touch1.clientX + touch2.clientX) / 2 - rect.left;
            const pinchCenterY = (touch1.clientY + touch2.clientY) / 2 - rect.top;

            selectedObject = null;

            for (let i = objects.length - 1; i >= 0; i--) {
                const object = objects[i];
                const relX = pinchCenterX - object.translateX;
                const relY = pinchCenterY - object.translateY;
                const angle = -object.rotation;
                const rotatedX = relX * Math.cos(angle) - relY * Math.sin(angle);
                const rotatedY = relX * Math.sin(angle) + relY * Math.cos(angle);
                const transformedX = rotatedX / object.scale;
                const transformedY = rotatedY / object.scale;
                const textHeight = object.ascent + object.descent;

                if (Math.abs(transformedX) <= object.width / 2 && Math.abs(transformedY) <= textHeight / 2) {
                    selectedObject = object;
                    lastSelectedTextObject = object;
                    fontSizeContainer.style.display = "block";
                    break;
                }

            }

            if (selectedObject === null) {
                isPinching = false;
                isDraggingObject = false;
                return;
            }

            pinchStartObjectState = {
                translateX: selectedObject.translateX,
                translateY: selectedObject.translateY,
                scale: selectedObject.scale,
                rotation: selectedObject.rotation,
                pinchCenterX: pinchCenterX,
                pinchCenterY: pinchCenterY,
                initialAngle: getAngle(touch1, touch2),
            };

            return;
        }

        const {offsetX, offsetY} = getEventCoordinates(event, rect);
        selectedObject = null;
        lastSelectedTextObject = null;

        for (let i = objects.length - 1; i >= 0; i--) {

            const object = objects[i];

            const relX = offsetX - object.translateX;
            const relY = offsetY - object.translateY;
            const angle = -object.rotation;
            const rotatedX = relX * Math.cos(angle) - relY * Math.sin(angle);
            const rotatedY = relX * Math.sin(angle) + relY * Math.cos(angle);
            const transformedX = rotatedX / object.scale;
            const transformedY = rotatedY / object.scale;
            const textHeight = object.ascent + object.descent;

            if (Math.abs(transformedX) <= object.width / 2 && Math.abs(transformedY) <= textHeight / 2) {

                isDraggingObject = true;
                selectedObject = object;
                lastSelectedTextObject = object;
                dragOffsetX = offsetX - object.translateX;
                dragOffsetY = offsetY - object.translateY;

                const visualSize = Math.round(selectedObject.fontSize * selectedObject.scale);
                fontSizeSlider.value = visualSize;
                fontSizeValue.textContent = visualSize;
                fontSizeContainer.style.display = "block";

                return;

            }

        }

    }

    function dragOrDraw(event) {
        event.preventDefault();

        if (isPinching && event.touches && event.touches.length === 2) {
            if (!selectedObject) return;

            const touch1 = event.touches[0];
            const touch2 = event.touches[1];
            const newDistance = getDistance(touch1, touch2);
            const scaleFactor = newDistance / initialPinchDistance;
            const currentAngle = getAngle(touch1, touch2);
            const rotationDelta = currentAngle - pinchStartObjectState.initialAngle;
            const currentPinchCenterX = (touch1.clientX + touch2.clientX) / 2 - rect.left;
            const currentPinchCenterY = (touch1.clientY + touch2.clientY) / 2 - rect.top;
            const deltaX = currentPinchCenterX - pinchStartObjectState.pinchCenterX;
            const deltaY = currentPinchCenterY - pinchStartObjectState.pinchCenterY;

            selectedObject.scale = pinchStartObjectState.scale * scaleFactor;
            selectedObject.rotation = pinchStartObjectState.rotation + rotationDelta;
            selectedObject.translateX = pinchStartObjectState.translateX + deltaX;
            selectedObject.translateY = pinchStartObjectState.translateY + deltaY;

            let visualSize = Math.round(selectedObject.fontSize * selectedObject.scale);
            const maxSize = 200;
            if (visualSize > maxSize) {
                selectedObject.scale = maxSize / selectedObject.fontSize;
                visualSize = maxSize;
                initialPinchDistance = newDistance;
                pinchStartObjectState.scale = selectedObject.scale;
            }

            fontSizeSlider.value = visualSize;
            fontSizeValue.textContent = visualSize;
            drawTextObjects(objects, objectCtx, rect);
            return;
        }

        const {offsetX, offsetY} = getEventCoordinates(event, rect);
        if (isDraggingObject && selectedObject) {
            selectedObject.translateX = offsetX - dragOffsetX;
            selectedObject.translateY = offsetY - dragOffsetY;
            drawTextObjects(objects, objectCtx, rect);
        }
    }

    function stopDragOrDrawing(event) {
        isPinching = false;
        initialPinchDistance = null;
        pinchStartObjectState = {};
        isDraggingObject = false;
        selectedObject = null;
    }

    // --- 이벤트 리스너 등록 ---
    colorButtons.forEach((button) => {
        button.addEventListener("click", () => {
            const newColor = window.getComputedStyle(button).backgroundColor;
            selectedColor = newColor;

            if (textInput.value !== "") {
                return;
            }

            if (lastSelectedTextObject) {
                lastSelectedTextObject.color = newColor;
                drawTextObjects(objects, objectCtx, rect);
            }
        });
    });

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

    });

    addTextBtn.addEventListener("click", () => {
        createTextBox(objects, obj => lastSelectedTextObject = obj, textInput, fontSizeContainer, fontSizeSlider, drawTextObjects, selectedColor, objectCtx, rect);
    });
    saveBtn.addEventListener("click", () => {
        // Blob 객체로 변환
        objectCanvas.toBlob(function (blob) {
            if (blob) {
                // Blob 객체를 FormData에 추가
                const formData = new FormData();
                formData.append("image", blob, "canvas.png");

                // 서버로 전송
                fetch("/profile/park/{profile_id}", {
                    method: "POST", body: formData,
                })
                    .then((response) => {
                        if (response.ok) {
                            alert("Park 작성 성공!");
                            // 성공 시 추가 작업
                        } else {
                            alert("저장 실패. 서버 오류가 발생했습니다.");
                            // 실패 시 추가 작업
                        }
                    })
                    .catch((error) => {
                        console.error("Error:", error);
                        alert("네트워크 오류가 발생했습니다.");
                    });
            }
        });
    });

    objectCanvas.addEventListener("mousedown", startDragOrDrawing);
    objectCanvas.addEventListener("mousemove", dragOrDraw);
    objectCanvas.addEventListener("mouseup", stopDragOrDrawing);
    objectCanvas.addEventListener("mouseleave", stopDragOrDrawing);
    objectCanvas.addEventListener("touchstart", startDragOrDrawing);
    objectCanvas.addEventListener("touchmove", dragOrDraw);
    objectCanvas.addEventListener("touchend", stopDragOrDrawing);

    initializeCanvases();
}