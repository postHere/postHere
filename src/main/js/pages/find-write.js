import {drawTextObjects} from "./common-find_park.js";
import {CanvasInteractionManager} from "./canvas-interaction.js";

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

    // --- 상태 변수 ---
    let selectedTextColor = "#E5C674"; // 텍스트 색상 (기본값: 80% 위치의 #E5C674)
    let selectedPenColor = "#678550";  // 그리기 펜 색상 (기본값: 90% 위치의 #678550)
    let selectedTextColorPosition = 0.8; // 텍스트 색상 슬라이더 위치 (0.0 ~ 1.0)
    let selectedPenColorPosition = 0.9;  // 그리기 펜 색상 슬라이더 위치 (0.0 ~ 1.0)

    let selectedFontSize = 16;
    const minFontSize = 12;
    const maxFontSize = 48;
    let hasDrawing = false;
    let objects = [];
    let lastSelectedTextObject = null; // 현재 수정 중인 텍스트 객체를 추적
    let isDrawingModeActive = false; // 그리기 모드 활성화 상태
    let penThickness = 3;
    const minPenThickness = 1; // 최소 펜 굵기
    const maxPenThickness = 20; // 최대 펜 굵기
    let selectedExpirationDate = null; // 선택된 만료 날짜를 저장할 변수

    // 컨테이너 요소
    const middleContainer = document.getElementById("middle-container");
    const toolBtnContainer = document.getElementById("tool-btn-container");

    // 버튼 요소
    const clearBtn = document.getElementById("btn-clear");
    const saveBtn = document.getElementById("btn-save");
    const backBtn = document.getElementById("back-btn");
    const toolTextBtn = document.getElementById("tool-text");
    const toolDrawBtn = document.getElementById("tool-draw"); // 그리기 버튼
    const drawIconUnselected = document.getElementById("tool-draw-unselected"); // 기본 아이콘
    const drawIconSelected = document.getElementById("tool-draw-selected"); // 선택 아이콘

    // 텍스트 입력 모달 UI
    const textInputOverlay = document.getElementById("text-input-overlay");
    const textInputForm = document.getElementById("text-input-form");
    const dynamicTextInput = document.getElementById("dynamic-text-input");
    const placeholderStyler = document.createElement('style');
    placeholderStyler.id = 'placeholder-styler';
    document.head.appendChild(placeholderStyler);

    // 편집 도구 UI
    const editingToolsContainer = document.getElementById("editing-tools-container");

    // 색상 슬라이더 UI
    const colorSliderWrapper = document.getElementById("color-slider-wrapper");
    const colorSliderTrack = document.getElementById("color-slider-track-wrapper");
    const colorSliderThumb = document.getElementById("color-slider-thumb-wrapper");

    // 그리기 선 굵기 & 폰트 사이즈 슬라이더 UI
    const weightSliderWrapper = document.getElementById("weight-slider-container");
    const weightSliderTrack = document.getElementById("weight-slider-track-wrapper");
    const weightSliderThumb = document.getElementById("weight-slider-thumb-wrapper");

    // --- 달력 관련 UI 요소 ---
    const expirationDateBtn = document.getElementById("tool-expiration-date");
    const datePickerInput = document.getElementById("date-picker-input"); // flatpickr를 연결할 숨겨진 input

    // flatpickr 인스턴스 생성 및 설정
    const flatpickrInstance = flatpickr(datePickerInput, {
        appendTo: document.body, // z-index 및 위치 문제를 피하기 위해 body에 직접 추가
        clickOutsideToClose: true, // 달력 바깥을 클릭하면 닫힘
        animate: true, // 애니메이션 활성화
        dateFormat: "Y-m-d", // 날짜 형식 지정

        // 달력이 열릴 때마다 실행되는 함수
        onOpen: function (selectedDates, dateStr, instance) {
            // 이전에 선택한 날짜가 있으면, 그 날짜를 달력에 표시
            if (selectedExpirationDate) {
                instance.setDate(selectedExpirationDate, false); // false 옵션으로 onChange 이벤트 방지
            }
        },
        // 날짜를 선택했을 때 실행되는 함수
        onChange: function (selectedDates, dateStr, instance) {
            // 선택된 날짜를 selectedExpirationDate 변수에 저장
            if (selectedDates.length > 0) {
                selectedExpirationDate = dateStr;
                // selectedExpirationDate = selectedDates[0];
                console.log("선택된 날짜:", selectedExpirationDate); // 정상적으로 저장되는지 확인용
            }
        },
    });

    // 달력 아이콘 버튼 클릭 시 flatpickr 달력을 열도록 이벤트 리스너 추가
    expirationDateBtn.addEventListener('click', () => {
        flatpickrInstance.open();
    });

    if (!paintCanvas.getContext || !objectCanvas.getContext || !imageCanvas.getContext) {
        alert("canvas context 불러오기 실패");
        return;
    }

    // 그리기 모드 토글 함수
    function toggleDrawingMode(forceOff = false) {
        isDrawingModeActive = forceOff ? false : !isDrawingModeActive;

        drawIconUnselected.classList.toggle('hidden', isDrawingModeActive);
        drawIconSelected.classList.toggle('hidden', !isDrawingModeActive);
        editingToolsContainer.classList.toggle('hidden', !isDrawingModeActive);
        editingToolsContainer.classList.toggle('hidden-placeholder', isDrawingModeActive);

        if (isDrawingModeActive) {
            history.pushState({drawingMode: true}, '', location.pathname);

            // --- 그리기 모드 진입 시 UI 업데이트 로직 변경 ---

            // 1. 굵기 슬라이더 위치 업데이트
            const weightPositionValue = 1 - ((penThickness - minPenThickness) / (maxPenThickness - minPenThickness));
            weightSliderThumb.style.top = `${weightPositionValue * 100}%`;

            // 2. 색상 슬라이더 위치를 '그리기 펜 색상' 위치로 업데이트
            const trackRect = colorSliderTrack.getBoundingClientRect();
            const initialColorX = trackRect.left + 6 + (selectedPenColorPosition * 270);
            updateSlider(initialColorX); // updateSlider를 호출하여 핸들 위치와 펜 색상을 모두 동기화

            // 3. 캔버스 컨텍스트에 펜 굵기 적용
            paintCtx.lineWidth = penThickness;
            // paintCtx.strokeStyle 설정은 updateSlider 함수가 처리하므로 여기서 중복으로 할 필요 없음

        }
    }

    /**
     * @description 캔버스에 콘텐츠가 있는지 확인하고, 버튼 상태를 업데이트.
     */
    function updateClearAndSaveBtnState() {

        // '저장' 버튼은 콘텐츠 유무로 활성화 여부를 결정.
        const hasContent = backgroundImage !== null || objects.length > 0 || hasDrawing;
        if (hasContent) {
            saveBtn.classList.remove("inactive");
            saveBtn.classList.add("active");
            saveBtn.disabled = false;
        } else {
            saveBtn.classList.add("inactive");
            saveBtn.classList.remove("active");
            saveBtn.disabled = true;
        }

        // '초기화' 버튼은 사용자가 직접 수정한 경우에만 보이도록 변경.
        if (isUserModified) {
            clearBtn.classList.remove("hidden");
        } else {
            clearBtn.classList.add("hidden");
        }

    }

    function clearCanvas() {

        paintCtx.clearRect(0, 0, paintCanvas.width, paintCanvas.height);
        objectCtx.clearRect(0, 0, objectCanvas.width, objectCanvas.height);

        hasDrawing = false;
        objects.length = 0;
        lastSelectedTextObject = null;
        isUserModified = false;

        if (interactionManager.selectedObject) {
            interactionManager.selectedObject = null;
        }

        const isOverwritePage = document.body.id === 'page-find-overwrite';
        if (!isOverwritePage) {
            imageCtx.clearRect(0, 0, imageCanvas.width, imageCanvas.height);
            backgroundImage = null;
        }

        updateClearAndSaveBtnState();

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
        });

        imageCtx.scale(scale, scale);
        paintCtx.scale(scale, scale);
        objectCtx.scale(scale, scale);

        // paintCtx.fillStyle = "#FFFFFF";
        // paintCtx.fillRect(0, 0, paintCanvas.width, paintCanvas.height);
        paintCtx.lineCap = "round";

        updateClearAndSaveBtnState();
    }

// --- 커스텀 색상 슬라이더 로직 (09.22 월 추가) ---
    const sliderColors = [
        { c: [255, 255, 255], p: 0.0 }, // 0% White (#FFFFFF)
        { c: [255, 0, 0],     p: 0.1 }, // 10% Red (#FF0000)
        { c: [242, 255, 0],   p: 0.2 }, // 20% Yellow (#F2FF00)
        { c: [255, 123, 0],   p: 0.3 }, // 30% Orange (#FF7B00)
        { c: [0, 255, 221],   p: 0.4 }, // 40% Cyan (#00FFDD)
        { c: [94, 255, 0],    p: 0.5 }, // 50% Green (#5EFF00)
        { c: [0, 77, 255],    p: 0.6 }, // 60% Blue (#004DFF)
        { c: [178, 0, 255],   p: 0.7 }, // 70% Purple (#B200FF)
        { c: [229, 198, 116], p: 0.8 }, // 80% Tan (#E5C674)
        { c: [103, 133, 80],  p: 0.9 }, // 90% Olive (#678550)
        { c: [0, 0, 0],       p: 1.0 }  // 100% Black (#000000)
    ];

    function interpolateColor(c1, c2, factor) {
        const result = c1.slice();
        for (let i = 0; i < 3; i++) {
            result[i] = Math.round(result[i] + factor * (c2[i] - c1[i]));
        }
        return `rgb(${result[0]}, ${result[1]}, ${result[2]})`;
    }

    function getColorForPosition(position) {
        for (let i = 0; i < sliderColors.length - 1; i++) {
            const start = sliderColors[i];
            const end = sliderColors[i + 1];
            if (position >= start.p && position <= end.p) {
                const factor = (position - start.p) / (end.p - start.p);
                return interpolateColor(start.c, end.c, factor);
            }
        }
        return `rgb(${sliderColors[sliderColors.length - 1].c.join(',')})`;
    }

    function updateSlider(x) {
        const wrapperRect = colorSliderWrapper.getBoundingClientRect();
        const paddingLeft = 6;
        const activeWidth = 270;
        const trackStart = wrapperRect.left + paddingLeft;
        const trackEnd = trackStart + activeWidth;
        const clampedX = Math.max(trackStart, Math.min(x, trackEnd));
        const thumbLeftPx = clampedX - wrapperRect.left;
        const positionValue = (clampedX - trackStart) / activeWidth;
        const newColor = getColorForPosition(positionValue);

        colorSliderThumb.style.left = `${thumbLeftPx}px`;

        // 현재 모드에 따라 다른 색상 변수를 업데이트
        if (isDrawingModeActive) {
            selectedPenColorPosition = positionValue;
            selectedPenColor = newColor;
            paintCtx.strokeStyle = selectedPenColor;
        } else {
            selectedTextColorPosition = positionValue;
            selectedTextColor = newColor;

            dynamicTextInput.style.color = selectedTextColor;
            const newStyle = `
            #dynamic-text-input::placeholder { color: ${selectedTextColor}; opacity: 0.7; }
            #dynamic-text-input::-webkit-input-placeholder { color: ${selectedTextColor}; opacity: 0.7; }
        `;
            placeholderStyler.textContent = newStyle;

            if (lastSelectedTextObject) {
                lastSelectedTextObject.color = selectedTextColor;
                lastSelectedTextObject.colorPosition = selectedTextColorPosition;
                drawTextObjects(objects, objectCtx, rect);
            }
        }
    }

    function onSliderMove(event) {
        event.preventDefault();
        const x = event.touches ? event.touches[0].clientX : event.clientX;
        updateSlider(x);
    }

    function onSliderEnd() {
        document.removeEventListener('mousemove', onSliderMove);
        document.removeEventListener('touchmove', onSliderMove);
        document.removeEventListener('mouseup', onSliderEnd);
        document.removeEventListener('touchend', onSliderEnd);
    }

    function onSliderStart(event) {
        onSliderMove(event);
        document.addEventListener('mousemove', onSliderMove);
        document.addEventListener('touchmove', onSliderMove);
        document.addEventListener('mouseup', onSliderEnd);
        document.addEventListener('touchend', onSliderEnd);
    }

    // --- 커스텀 색상 슬라이더 로직 종료 ---

    // 선 굵기 & 폰트 사이즈 슬라이더
    function updateWeightSlider(y) {
        const trackRect = weightSliderTrack.getBoundingClientRect();
        const clampedY = Math.max(trackRect.top, Math.min(y, trackRect.bottom));
        const positionValue = (clampedY - trackRect.top) / trackRect.height;

        weightSliderThumb.style.top = `${positionValue * 100}%`;

        // 현재 모드에 따라 다른 값을 업데이트
        if (!textInputOverlay.classList.contains('hidden')) { // 텍스트 모드일 때
            selectedFontSize = minFontSize + (1 - positionValue) * (maxFontSize - minFontSize);
            dynamicTextInput.style.fontSize = `${selectedFontSize}px`;
            if (lastSelectedTextObject) {
                lastSelectedTextObject.fontSize = selectedFontSize;
                drawTextObjects(objects, objectCtx, rect);
            }
        } else if (isDrawingModeActive) { // 그리기 모드일 때
            penThickness = minPenThickness + (1 - positionValue) * (maxPenThickness - minPenThickness);
            paintCtx.lineWidth = penThickness;
        }
    }

    function onWeightSliderMove(event) {
        event.preventDefault();
        const y = event.touches ? event.touches[0].clientY : event.clientY;
        updateWeightSlider(y);
    }

    function onWeightSliderEnd() {
        document.removeEventListener('mousemove', onWeightSliderMove);
        document.removeEventListener('touchmove', onWeightSliderMove);
        document.removeEventListener('mouseup', onWeightSliderEnd);
        document.removeEventListener('touchend', onWeightSliderEnd);
    }

    function onWeightSliderStart(event) {
        onWeightSliderMove(event); // 클릭 즉시 반영
        document.addEventListener('mousemove', onWeightSliderMove);
        document.addEventListener('touchmove', onWeightSliderMove);
        document.addEventListener('mouseup', onWeightSliderEnd);
        document.addEventListener('touchend', onWeightSliderEnd);
    }

    // ▲▲▲ 선 굵기 & 폰트 사이즈 슬라이더 로직 종료 ▲▲▲

    // --- 텍스트 모달 관련 함수 (09.22 월 추가) ---
    function showTextInput(objectToEdit = null) {
        textInputOverlay.classList.remove('hidden');
        editingToolsContainer.classList.remove('hidden');

        // --- 수정 모드 ---
        if (objectToEdit) {
            // ... (수정 모드 로직은 그대로 유지)
            lastSelectedTextObject = objectToEdit;
            selectedFontSize = objectToEdit.fontSize;
            // 변수명 변경: selectedColorPosition -> selectedTextColorPosition
            selectedTextColorPosition = objectToEdit.colorPosition || 0.8;

            dynamicTextInput.value = objectToEdit.text;

            const initialFontPos = 1 - ((selectedFontSize - minFontSize) / (maxFontSize - minFontSize));
            weightSliderThumb.style.top = `${initialFontPos * 100}%`;
            dynamicTextInput.style.fontSize = `${selectedFontSize}px`;

            const trackRect = colorSliderTrack.getBoundingClientRect();
            // 변수명 변경: selectedColorPosition -> selectedTextColorPosition
            const initialColorX = trackRect.left + 6 + (selectedTextColorPosition * 270);
            updateSlider(initialColorX);

        } else { // --- 생성 모드 ---
            lastSelectedTextObject = null;
            dynamicTextInput.value = '';

            selectedFontSize = 16;
            const initialFontPos = 1 - ((selectedFontSize - minFontSize) / (maxFontSize - minFontSize));
            weightSliderThumb.style.top = `${initialFontPos * 100}%`;
            dynamicTextInput.style.fontSize = `${selectedFontSize}px`;

            // 생성 모드일 때, 텍스트 색상 슬라이더 위치를 기본값(80%)으로 설정
            const trackRect = colorSliderTrack.getBoundingClientRect();
            const initialColorX = trackRect.left + 6 + (selectedTextColorPosition * 270);
            updateSlider(initialColorX);
        }

        dynamicTextInput.focus();
    }

    function commitDynamicText() {
        const text = dynamicTextInput.value;

        if (lastSelectedTextObject) { // 수정 모드일 때
            if (text) { // 텍스트가 있으면 속성 업데이트
                lastSelectedTextObject.text = text;
                lastSelectedTextObject.fontSize = selectedFontSize;
                // 텍스트 색상 변수 사용
                lastSelectedTextObject.color = selectedTextColor;
                lastSelectedTextObject.colorPosition = selectedTextColorPosition;
            } else { // 텍스트를 지웠으면 객체 삭제
                objects.splice(objects.indexOf(lastSelectedTextObject), 1);
            }
            isUserModified = true; // 텍스트 수정/삭제 시
        } else if (text) { // 생성 모드이고 텍스트가 있을 때
            const textBox = {
                type: 'text',
                text: text,
                translateX: rect.width / 2,
                translateY: rect.height / 2,
                // 텍스트 색상 변수 사용
                color: selectedTextColor,
                fontSize: selectedFontSize,
                colorPosition: selectedTextColorPosition,
                scale: 1,
                rotation: 0,
                width: 0,
                ascent: 0,
                descent: 0,
            };
            objects.push(textBox);
            isUserModified = true; // 새 텍스트 생성 시
        }

        drawTextObjects(objects, objectCtx, rect);
        updateClearAndSaveBtnState();
        textInputOverlay.classList.add('hidden');
        editingToolsContainer.classList.add('hidden');
        lastSelectedTextObject = null;
    }

    // --- 텍스트 모달 관련 종료 (09.22 월 추가) ---
    // 하기 함수 내 텍스트 모달 관련 변경사항 있음.
    const interactionConfig = {
        isInteractionDisabled: () => !textInputOverlay.classList.contains('hidden'),

        getBackgroundImage: () => backgroundImage,

        findSelectableObject: (coord) => {
            // 그리기 모드이거나 텍스트 입력창이 열려있으면 객체 선택 안 함
            if (isDrawingModeActive || !textInputOverlay.classList.contains('hidden')) return null;

            // ... (기존 객체 찾는 로직은 동일)
            for (let i = objects.length - 1; i >= 0; i--) {
                const object = objects[i];
                if (object.type !== 'text') continue;

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
            return null;
        },
        onObjectSelect: (selectedObject) => {
            if (selectedObject.type === 'text') {
                showTextInput(selectedObject);
            }
        },
        onObjectMove: () => {
            drawTextObjects(objects, objectCtx, rect);
        },

        // 그리기 시작 콜백 구현
        onDrawStart: () => {
            if (!isDrawingModeActive) return;
            paintCtx.beginPath();
        },
        // 그리는 중 콜백 구현
        onDrawMove: (from, to) => {
            if (!isDrawingModeActive) return;
            hasDrawing = true;
            isUserModified = true;
            paintCtx.moveTo(from.x, from.y);
            paintCtx.lineTo(to.x, to.y);
            paintCtx.stroke();
            updateClearAndSaveBtnState();
        },
        onPinchUpdate: (selectedObject) => { /* 기존 코드 유지 */
        },
        onInteractionEnd: () => {
        }
    };

    // --- Visual Viewport API를 사용한 키보드 대응 로직 ---
    function handleViewportResize() {
        // 현재 실제로 보이는 화면 영역의 정보를 가져옵니다.
        const viewport = window.visualViewport;

        // 전체 창 높이와 실제 보이는 영역의 높이 차이를 이용해 키보드 노출 여부를 판단합니다.
        // (150px 이상 차이나면 키보드가 올라온 것으로 간주)
        const isKeyboardVisible = window.innerHeight > viewport.height + 150;

        // 1. 키보드가 나타나면 #tool-btn-container를 숨기고, 사라지면 다시 표시합니다.
        toolBtnContainer.classList.toggle('hidden', isKeyboardVisible);

        // 2. 키보드가 나타나면 #middle-container를 위로(-50px) 올리고, 사라지면 원위치합니다.
        if (isKeyboardVisible) {

            middleContainer.style.transform = 'translateY(-100px)';
            // 보이는 영역의 높이와 상단 위치를 계산합니다.
            const visibleHeight = `${viewport.height}px`;
            const topOffset = `${viewport.offsetTop}px`;
            textInputOverlay.style.top = topOffset;
            textInputOverlay.style.height = visibleHeight;
            editingToolsContainer.style.top = topOffset;
            editingToolsContainer.style.height = visibleHeight;

        } else {
            middleContainer.style.transform = 'translateY(0)';

            // 에디팅 UI들의 top, height 스타일을 제거하여 원래 CSS 값으로 복원합니다.
            textInputOverlay.style.top = '';
            textInputOverlay.style.height = '';
            editingToolsContainer.style.top = '';
            editingToolsContainer.style.height = '';
        }



        // 1. 텍스트 입력 오버레이의 위치와 크기를 보이는 영역에 맞춥니다.
        //    이렇게 하면 flex 중앙 정렬이 보이는 영역의 중앙을 기준으로 동작합니다.
        if (!textInputOverlay.classList.contains('hidden')) {

        }

        // 2. 편집 도구 컨테이너의 위치와 크기도 보이는 영역에 맞춥니다.
        //    이렇게 하면 슬라이더가 키보드 위쪽에 위치하게 됩니다.
        if (!editingToolsContainer.classList.contains('hidden')) {

        }
    }
    // 키보드가 나타나거나 사라질 때(화면 크기가 변할 때)마다 핸들러 함수를 호출합니다.
    window.visualViewport.addEventListener('resize', handleViewportResize);

    // --- 이벤트 리스너 등록 ---
    // 색상 선택 슬라이더 추가 완료 시 조절 필요.
    clearBtn.addEventListener("click", clearCanvas);
    backBtn.addEventListener("click", () => {
        history.back(); // 브라우저 내 뒤로 가기 기능.
    })

    // --- 텍스트 모달화 관련 추가 리스너 ---
    // 새 텍스트 도구 버튼 클릭 시 모달 표시
    toolTextBtn.addEventListener('click', () => {
        // 만약 그리기 모드가 활성화된 상태라면,
        if (isDrawingModeActive) {
            // 그리기 모드를 강제로 비활성화합니다.
            toggleDrawingMode(true);
        }
        // 그 후, 텍스트 입력창을 띄웁니다.
        showTextInput(null);
    });

    // 슬라이더 이벤트
    colorSliderWrapper.addEventListener('mousedown', onSliderStart);
    colorSliderWrapper.addEventListener('touchstart', onSliderStart);
    weightSliderWrapper.addEventListener('mousedown', onWeightSliderStart);
    weightSliderWrapper.addEventListener('touchstart', onWeightSliderStart);

    // 전체 배경인 오버레이에 리스너를 연결
    textInputOverlay.addEventListener('click', (event) => {
        // 클릭된 대상이 UI요소가 아닌 오버레이 자신일 때만 실행
        if (event.target === textInputOverlay) {
            commitDynamicText();
        }
    });
    textInputForm.addEventListener('submit', (event) => {
        // form의 기본 동작(페이지 새로고침)을 막습니다.
        event.preventDefault();
        // 기존의 텍스트 삽입 함수를 호출합니다.
        commitDynamicText();
    });

    // 실시간 텍스트 미리보기를 위한 리스너
    dynamicTextInput.addEventListener('input', () => {
        if (lastSelectedTextObject) {
            lastSelectedTextObject.text = dynamicTextInput.value;
            drawTextObjects(objects, objectCtx, rect);
        }
    });

    // 텍스트 모달화 관련 추가 리스너 (종료)

    // 그리기 버튼 클릭 이벤트
    toolDrawBtn.addEventListener('click', () => toggleDrawingMode());

    // 브라우저 뒤로가기(모바일 포함) 이벤트 감지
    window.addEventListener('popstate', (event) => {
        if (isDrawingModeActive) {
            // 그리기 모드가 활성화된 상태에서 뒤로가기가 발생하면 모드를 강제로 끔
            toggleDrawingMode(true);
        }
    });

    /**
     * @description 현재 캔버스 상태를 이미지로 변환하여 서버에 전송합니다.
     */
    async function saveCanvasAsImage() {
        // 1. 버튼 비활성화 (중복 클릭 방지)
        saveBtn.disabled = true;
        saveBtn.textContent = 'Saving...';

        // 2. 임시 캔버스를 생성하여 3개의 캔버스를 하나로 합칩니다.
        const mergedCanvas = document.createElement('canvas');
        mergedCanvas.width = imageCanvas.width;
        mergedCanvas.height = imageCanvas.height;
        const mergedCtx = mergedCanvas.getContext('2d');

        // 3. 캔버스를 순서대로 그립니다. (배경 -> 페인트 -> 텍스트/객체)
        mergedCtx.drawImage(imageCanvas, 0, 0);
        mergedCtx.drawImage(paintCanvas, 0, 0);
        mergedCtx.drawImage(objectCanvas, 0, 0);

        // 4. 합쳐진 캔버스를 Blob 객체로 변환합니다.
        mergedCanvas.toBlob(async (blob) => {
            if (!blob) {
                alert('이미지 변환에 실패했습니다.');
                saveBtn.disabled = false;
                saveBtn.textContent = 'share';
                return;
            }

            const formData = new FormData();
            formData.append('content_capture', blob, 'find-write.png');

            if (selectedExpirationDate) {
                formData.append('expiration_date', selectedExpirationDate);
            }

            try {
                const response = await fetch('/find', {
                    method: 'POST',
                    body: formData,
                });

                if (!response.ok) {
                    throw new Error(`Server responded with ${response.status}: ${response.statusText}`);
                }

                const result = await response.json();
                console.log('서버 응답:', result);

                // 저장 성공 시 페이지 이동 (리다이렉트)
                window.location.href = '/map';

            } catch (error) {
                console.error('전송 중 오류 발생:', error);
                alert('저장 중 오류가 발생했습니다. 다시 시도해 주세요.');

                // 오류 발생 시에는 버튼을 다시 활성화
                saveBtn.disabled = false;
                saveBtn.textContent = 'share';
            }

        }, 'image/png', 0.95);
    }

    saveBtn.addEventListener('click', saveCanvasAsImage);

    // --- 초기 실행 ---
    initializeCanvases();
    const interactionManager = new CanvasInteractionManager(objectCanvas, objects, interactionConfig);
    interactionManager.registerEvents();

    // 다른 모듈에서 이미지 기능을 추가할 수 있도록 주요 객체들을 반환.
    return {interactionManager, imageCtx, rect, updateSaveBtnState: updateClearAndSaveBtnState, toggleDrawingMode};
}

// -------------------
let backgroundImage = null;
let isUserModified = false; // 사용자가 직접 수정했는지 여부를 추적하는 플래그

// ---
export function processLoadedImage(image, rect) {
    const maxWidth = rect.width;
    const maxHeight = rect.height;
    let {width, height} = image;
    if (width > maxWidth || height > maxHeight) {
        const ratio = Math.min(maxWidth / width, maxHeight / height);
        width *= ratio;
        height *= ratio;
    }
    return {
        type: 'image',
        image: image,
        originalWidth: width,
        originalHeight: height,
        translateX: rect.width / 2,
        translateY: rect.height / 2,
        scale: 1,
        rotation: 0
    };
}

// ---
export function drawImageObject(imageCtx, rect) {
    if (!backgroundImage) return;
    imageCtx.clearRect(0, 0, rect.width, rect.height);
    imageCtx.save();
    imageCtx.translate(backgroundImage.translateX, backgroundImage.translateY);
    imageCtx.rotate(backgroundImage.rotation);
    imageCtx.scale(backgroundImage.scale, backgroundImage.scale);
    imageCtx.drawImage(backgroundImage.image, -backgroundImage.originalWidth / 2, -backgroundImage.originalHeight / 2, backgroundImage.originalWidth, backgroundImage.originalHeight);
    imageCtx.restore();
}

// ---
export function setAndDrawBackgroundImage(image, imageCtx, rect) {
    backgroundImage = processLoadedImage(image, rect);
    drawImageObject(imageCtx, rect);
}

/**
 * initFindWrite로 생성된 캔버스에 이미지 추가 및 제어 기능을 설정합니다.
 * @param {{interactionManager: CanvasInteractionManager, imageCtx: CanvasRenderingContext2D, rect: DOMRect}} deps
 * - initFindWrite에서 반환된 의존성 객체.
 */
export function setupImageControls({interactionManager, imageCtx, rect, updateSaveBtnState, toggleDrawingMode}) {

    const addImageBtn = document.getElementById("tool-image");
    const imageLoader = document.getElementById("image-loader");


    function loadImageFromLocal(event) {
        const file = event.target.files[0];
        if (!file) return;
        const reader = new FileReader();
        reader.onload = (e) => {
            const image = new Image();
            image.onload = () => {
                setAndDrawBackgroundImage(image, imageCtx, rect);
                isUserModified = true;
                updateSaveBtnState();
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
            drawImageObject(imageCtx, rect);
        } else {
            // 이미지가 아니면 기존 이동 로직(텍스트)을 실행합니다.
            originalOnObjectMove();
        }
    };

    addImageBtn.addEventListener("click", () => {
        // setupTextAndDrawControls에서 toggleDrawingMode 함수를 잘 받아왔는지 확인하고,
        // 그리기 모드가 활성화 상태인지 확인합니다. (isDrawingModeActive는 직접 접근 불가하므로, 함수가 넘어왔는지로 간접 확인)
        // 실제로는 isDrawingModeActive 상태를 알 수 없지만, 토글 함수를 호출해도 UI상 문제가 없으므로 그냥 호출합니다.
        // 더 정확한 방법은 isDrawingModeActive 상태도 넘겨주는 것이지만, 현재 구조에서는 이 방법이 간단합니다.
        if (interactionManager.config.findSelectableObject({x: 0, y: 0}) === null) {
            toggleDrawingMode(true);
        }

        imageLoader.click()
    });

    imageLoader.addEventListener("change", loadImageFromLocal);
}

export function initFindWrite() {
    // --- 페이지 로드 시 최종 실행 ---
    const dependencies = setupTextAndDrawControls();
    setupImageControls(dependencies);
}

// 로컬 테스트용 코드
// initFindWrite();