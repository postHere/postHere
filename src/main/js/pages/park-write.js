import {createTextBox, getAngle, getDistance, getEventCoordinates, drawTextObjects} from "./common-find_park.js";
import {CanvasInteractionManager} from "./canvas-interaction";

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

    const interactionConfig = {
        findSelectableObject: (coord) => {
            if (textInput.value !== "") return null;

            // 텍스트 객체만 탐색
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
            return null; // 배경 이미지나 그리기가 없으므로, 못 찾으면 끝.
        },
        onObjectSelect: (selectedObject) => {
            lastSelectedTextObject = selectedObject;
            const visualSize = Math.round(selectedObject.fontSize * selectedObject.scale);
            fontSizeSlider.value = visualSize;
            fontSizeValue.textContent = visualSize;
            fontSizeContainer.style.display = "block";
        },
        onObjectMove: () => {
            drawTextObjects(objects, objectCtx, rect); // 텍스트 객체만 다시 그리면 됨
        },
        onPinchUpdate: (selectedObject) => {
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
        onInteractionEnd: () => {
            // 여기선 특별히 할 작업 없음
        }
        // onDrawStart, onDrawMove는 필요 없으므로 정의하지 않음
    };

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

    initializeCanvases();
    const interactionManager = new CanvasInteractionManager(objectCanvas, objects, interactionConfig);
    interactionManager.registerEvents();

}