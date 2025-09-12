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

    /**
     * @description 두 터치 지점 사이의 유클리드 거리를 계산합니다.
     * @param {Touch} touch1
     * @param {Touch} touch2
     * @return {number} 두 지점 사이의 거리.
     */
    function getDistance(touch1, touch2) {

        // clientX, Y는 웹 브라우저 창 전체를 기준으로 한 좌표.
        const dx = touch1.clientX - touch2.clientX;
        const dy = touch1.clientY - touch2.clientY;
        return Math.sqrt(dx * dx + dy * dy);

    }

    /**
     * @description 두 지점 사이의 각도를 계산합니다. (라디안)
     * @param {Touch} touch1
     * @param {Touch} touch2
     * @return {number} 각도 (라디안)
     */
    function getAngle(touch1, touch2) {
        const dx = touch1.clientX - touch2.clientX;
        const dy = touch1.clientY - touch2.clientY;
        return Math.atan2(dy, dx);
    }

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
        imageCtx.drawImage(
            backgroundImage.image,
            -backgroundImage.originalWidth / 2, // x(좌측 끝 좌표) = 기존값(캔버스 중앙 x좌표) - 그림 너비의 절반값
            -backgroundImage.originalHeight / 2,
            backgroundImage.originalWidth,
            backgroundImage.originalHeight,
        )
        // 크기가 넘칠 경우, 캔버스 크기에 알맞게 조절하는 로직은 아래 loadImage 메소드에서 담당.

        // 5. 캔버스 상태 복원 (이미지 제외 타 객체 상태 복원)
        imageCtx.restore();

    }


    // 텍스트 레이어(objectCanvas)만 다시 그리는 함수
    function createTextBox() {

        const text = textInput.value;
        if (!text) return;

        const textBox = {
            type: 'text',
            text: text,

            // 캔버스 내 텍스트 객체 좌측 끝 좌표. 캔버스 축을 이동하는 translate와는 별개.
            translateX: 50,
            translateY: 50,
            color: selectedColor,
            fontSize: parseInt(fontSizeSlider.value),
            scale: 1,
            rotation: 0,
            width: 0,   // 측정값을 저장할 속성 초기화
            ascent: 0,
            descent: 0,
        };

        objects.push(textBox);
        lastSelectedTextObject = textBox;
        textInput.value = "";
        fontSizeContainer.style.display = "block";

        drawTextObjects();
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
                    type: 'image',
                    image: image,
                    originalWidth: width,
                    originalHeight: height,
                    // 이미지 삽입 시 가운데 정렬하기 위한 x, y 좌표 계산.
                    // translateX는 이미지의 '중심점'이 캔버스에서 위치하는 'x좌표'.
                    translateX: rect.width / 2,   // 캔버스 중앙 x좌표 (가운데 정렬은 imageCtx.drawImage()에서 처리)
                    translateY: rect.height / 2,
                    // width: width,   // 최종 조절된 너비.
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

    function drawTextObjects() {
        objectCtx.clearRect(0, 0, rect.width, rect.height);

        objects.forEach(object => {

            // 1. 캔버스 상태 저장
            objectCtx.save();

            // 2. 캔버스 변형 적용 (이동, 회전, 확대/축소)
            objectCtx.translate(object.translateX, object.translateY);
            objectCtx.rotate(object.rotation);
            objectCtx.scale(object.scale, object.scale);

            // 3. 텍스트 속성 설정 (fontSize는 기본값 적용)
            objectCtx.font = `${object.fontSize}px sans-serif`;
            objectCtx.fillStyle = object.color;

            // 4. 텍스트 크기 측정 (히트박스 및 중앙 정렬에 사용)
            const metrics = objectCtx.measureText(object.text);
            object.width = metrics.width;
            object.ascent = metrics.actualBoundingBoxAscent;
            object.descent = metrics.actualBoundingBoxDescent;

            // 5. 텍스트 중앙 정렬하여 그리기 (x = 0, y = 0을 중심으로)
            objectCtx.textAlign = "center";
            objectCtx.textBaseline = "middle";
            objectCtx.fillText(object.text, 0, 0);

            // 6. 캔버스 상태 복원
            objectCtx.restore();

        });

    }


    function startDragOrDrawing(event) {

        // --- [1] 두 손가락 제스처(핀치 줌 / 회전 / 패닝) 처리 ---
        if (event.touches && event.touches.length === 2) {

            isPinching = true;
            isDrawing = false;
            isDraggingObject = true;
            const touch1 = event.touches[0];
            const touch2 = event.touches[1];
            initialPinchDistance = getDistance(touch1, touch2);
            const pinchCenterX = (touch1.clientX + touch2.clientX) / 2 - rect.left;
            const pinchCenterY = (touch1.clientY + touch2.clientY) / 2 - rect.top;

            // --- 1-1. 텍스트 객체 우선 탐색 ---
            selectedObject = null;  // 선택 초기화

            for (let i = objects.length - 1; i >= 0; i--) {

                const object = objects[i];

                // 텍스트 객체애 대한 역변환 히트 테스트.
                const relX = pinchCenterX - object.translateX;
                const relY = pinchCenterY - object.translateY;
                const angle = -object.rotation;
                const rotatedX = relX * Math.cos(angle) - relY * Math.sin(angle);
                const rotatedY = relX * Math.sin(angle) + relY * Math.cos(angle);
                const transformedX = rotatedX / object.scale;
                const transformedY = rotatedY / object.scale;

                const textHeight = object.ascent + object.descent;  // 텍스트의 실제 높이.

                // (이미지의 중심이 0, 0이므로 너비/높이의 절반과 비교)
                if (Math.abs(transformedX) <= object.width / 2
                    && Math.abs(transformedY) <= textHeight / 2) {

                    selectedObject = object;            // 텍스트 객체 선택.
                    lastSelectedTextObject = object;    // UI 연동을 위해 저장.
                    fontSizeContainer.style.display = "block";
                    break;

                }

            }

            // --- 1-2. 텍스트가 없으면 배경 이미지 선택 ---
            if (selectedObject === null) {
                if (backgroundImage) {

                    // 텍스트 객체가 선택되지 않았으나 배경 이미지가 있으면 배경 이미지 선택.
                    selectedObject = backgroundImage;
                    fontSizeContainer.style.display = "none";

                } else {

                    // 택스트 객체도 선택되지 않고, 배경 이미지도 없으면 조작 종료.
                    isPinching = false;
                    isDraggingObject = false;
                    return;

                }

            }

            // --- 1-3. 선택된 객체(텍스트 또는 이미지)의 초기 상태 저장 ---

            pinchStartImageState = {

                translateX: selectedObject.translateX, // 처음에는 캔버스 중앙 x좌표
                translateY: selectedObject.translateY,
                scale: selectedObject.scale,
                rotation: selectedObject.rotation,
                pinchCenterX: pinchCenterX,
                pinchCenterY: pinchCenterY,
                initialAngle: getAngle(touch1, touch2),

            };


            return; // 두 손가락 조작(핀치 줌 모드)이므로, 아래의 한 손가락 로직(단일 클릭/터치)은 실행하지 않음.

        }

        // --- [2] 단일 제스처 (드래그 / 그리기) 로직 ---
        // 클릭/터치 좌표 구하기 [페이지 좌상단 ~ 클릭/터치 위치]
        const {offsetX, offsetY} = getEventCoordinates(event);
        selectedObject = null;
        lastSelectedTextObject = null;

        // --- 2-1. 텍스트 객체 우선 탐색 (단일 클릭 / 터치) ---
        // 텍스트 객체(최상단 레이어)를 클릭했는지 확인 (가장 위 레이어부터)
        for (let i = objects.length - 1; i >= 0; i--) {

            const object = objects[i];

            // 텍스트 객체에 대한 역변환 히트 테스트 (핀치 줌과 동일한 로직)
            const relX = offsetX - object.translateX;
            const relY = offsetY - object.translateY;
            const angle = -object.rotation;
            const rotatedX = relX * Math.cos(angle) - relY * Math.sin(angle);
            const rotatedY = relX * Math.sin(angle) + relY * Math.cos(angle);
            const transformedX = rotatedX / object.scale;
            const transformedY = rotatedY / object.scale;

            const textHeight = object.ascent + object.descent;

            if (Math.abs(transformedX) <= object.width / 2
                && Math.abs(transformedY) <= textHeight / 2) {

                isDraggingObject = true;
                selectedObject = object;
                lastSelectedTextObject = object;

                // 마우스와 객체 좌측 끝의 간격.
                dragOffsetX = offsetX - object.translateX; // 간격[텍스트 객체 좌상단 ~ 클릭/터치]
                dragOffsetY = offsetY - object.translateY;

                // 슬라이더 UI 업데이트 (시각적 크기 반영)
                const visualSize = Math.round(selectedObject.fontSize * selectedObject.scale);
                fontSizeSlider.value = visualSize;
                fontSizeValue.textContent = visualSize;
                fontSizeContainer.style.display = "block";

                return;     // 텍스트 선택 성공.

            }

        }

        // --- 2-2. 배경 이미지 탐색 (단일 클릭/터치) ---
        // 2순위: 텍스트를 클릭하지 않았다면, 배경 이미지(최하단 레이어)를 클릭했는지 확인.
        if (backgroundImage) {

            // --- 클릭/터치 좌표 역변환 & 이미지 변형 이전 좌표 기준 클릭/터치 감지 로직 ---
            // 1. 클릭 좌표를 이미지 중심으로 이동한 상대 좌표로 변환.
            const relX = offsetX - backgroundImage.translateX;
            const relY = offsetY - backgroundImage.translateY;

            // 2. 클릭 좌표에 역회전(negative rotation)을 적용.
            const angle = -backgroundImage.rotation;    // 반대 방향으로 회전.
            const rotatedX = relX * Math.cos(angle) - relY * Math.sin(angle);
            const rotatedY = relX * Math.sin(angle) + relY * Math.cos(angle);

            // 3. 클릭 좌표에 역스케일(inverse scale)을 적용.
            const transformedX = rotatedX / backgroundImage.scale;
            const transformedY = rotatedY / backgroundImage.scale;

            // 4. 변화된 좌표가 이미지 변형 이전 경계 내에 있는지 확인.
            // (이미지의 중심이 0, 0이므로 너비/높이의 절반과 비교)
            if (Math.abs(transformedX) <= backgroundImage.originalWidth / 2
                && Math.abs(transformedY) <= backgroundImage.originalHeight / 2) {

                // --- 이미지 선택 성공 ---
                isDraggingObject = true;
                selectedObject = backgroundImage;   // 선택된 객체로 이미지 지정.
                dragOffsetX = offsetX - backgroundImage.translateX;
                dragOffsetY = offsetY - backgroundImage.translateY;

                // 배경 이미지를 선택했으므로, 그리기 모드를 시작하지 않고 메소드 종료.
                fontSizeContainer.style.display = "none";
                return;

            }

        }

        // --- 2-3. 그리기 모드 시작 ---
        // 3순위: 아무 객체로 선택하지 않았다면 그리기 모드 시작.
        isDrawing = true;
        fontSizeContainer.style.display = "none";
        [lastX, lastY] = [offsetX, offsetY];

    }

    function getEventCoordinates(event) {

        if (event.touches) {
            const touch = event.touches[0];
            return {
                // clientX, Y는 웹 브라우저 창 전체를 기준으로 한 좌표.
                // 캔버스 기준(내부)의 좌표를 구하기 위해서는, 캔버스의 왼쪽 여백(rect.left)만큼 빼야 함.
                offsetX: touch.clientX - rect.left,
                offsetY: touch.clientY - rect.top,
            };
        }

        return {offsetX: event.offsetX, offsetY: event.offsetY};
    }

    function dragOrDraw(event) {

        event.preventDefault(); // 모바일 환경에서의 스크롤 등 방지.

        // --- 핀치 줌 동작 중일 때 이미지 크기 및 위치 조절 ---
        if (isPinching && event.touches && event.touches.length === 2) {
            if (!selectedObject) return;   // 배경 이미지가 없으면 실행하지 않음.

            const touch1 = event.touches[0];
            const touch2 = event.touches[1];

            // --- 1. 스케일 계산 ---
            const newDistance = getDistance(touch1, touch2);
            const scaleFactor = newDistance / initialPinchDistance;

            // --- 2. 회전 계산 ---
            const currentAngle = getAngle(touch1, touch2);
            const rotationDelta = currentAngle - pinchStartImageState.initialAngle; // 회전 변화량.

            // --- 3. 이동(Pan) 계산 ---
            // 핀치 제스처의 중심점을 계산
            const currentPinchCenterX = (touch1.clientX + touch2.clientX) / 2 - rect.left;
            const currentPinchCenterY = (touch1.clientY + touch2.clientY) / 2 - rect.top;
            const deltaX = currentPinchCenterX - pinchStartImageState.pinchCenterX;
            const deltaY = currentPinchCenterY - pinchStartImageState.pinchCenterY;

            // selectedObject의 속성을 업데이트.
            selectedObject.scale = pinchStartImageState.scale * scaleFactor;
            selectedObject.rotation = pinchStartImageState.rotation + rotationDelta;
            selectedObject.translateX = pinchStartImageState.translateX + deltaX;
            selectedObject.translateY = pinchStartImageState.translateY + deltaY;

            // selectedObject의 타입에 따라 적절한 그리기 함수 호출.
            if (selectedObject.type === 'text') {

                // --- 핀치 줌 중 UI 업데이트 ---
                // 1. 시각적 글자 크기 계산.
                let visualSize = Math.round(selectedObject.fontSize * selectedObject.scale);

                // 2. 최대 크기 제한 적용 (200px).
                const maxSize = 200;
                if (visualSize > maxSize) {

                    // fontSize, maxSize 기준 최대 scale 값 계산.
                    selectedObject.scale = maxSize / selectedObject.fontSize;
                    visualSize = maxSize;   // UI 표시값 최대값으로 고정.
                    initialPinchDistance = newDistance;
                    pinchStartImageState.scale = selectedObject.scale;

                }

                // 3. 슬라이더와 텍스트 값에 반영.
                fontSizeSlider.value = visualSize;
                fontSizeValue.textContent = visualSize;

                drawTextObjects();

            } else if (selectedObject.type === 'image') {

                drawImageObject();

            }

            return;             // 핀치 줌 로직만 실행하고 함수 종료.

        }

        // --- 단일 클릭/터치를 통한 드래그/그리기 로직 ---
        const {offsetX, offsetY} = getEventCoordinates(event);

        if (isDraggingObject && selectedObject) {

            // 드래그 중인 객체의 새로운 x, y 위치(객체의 좌상단 시작점)
            // = 클릭(터치) 위치 좌표 - 간격[드래그 객체 좌상단 좌표 ~ 클릭 위치 좌표]
            selectedObject.translateX = offsetX - dragOffsetX;
            selectedObject.translateY = offsetY - dragOffsetY;

            // 선택된 객체의 타입에 따라 다른 그리기 함수를 호출.
            if (selectedObject.type === 'text') {
                drawTextObjects();      // 텍스트 객체들을 다시 그림.
            } else if (selectedObject.type === 'image') {
                drawImageObject();      // 배경 이미지를 다시 그림.
            }

        } else if (isDrawing) {

            paintCtx.strokeStyle = selectedColor;
            paintCtx.lineWidth = penThickness;
            paintCtx.beginPath();
            paintCtx.moveTo(lastX, lastY);
            paintCtx.lineTo(offsetX, offsetY);
            paintCtx.stroke();      // 그림 레이어에만 그리기
            [lastX, lastY] = [offsetX, offsetY];

        }

    }

    function stopDragOrDrawing(event) {

        // [추가] 핀치 줌 상태 초기화.
        isPinching = false;
        initialPinchDistance = null;
        pinchStartImageState = {};

        // 그리기 관련 로직 초기화.
        isDrawing = false;
        isDraggingObject = false;
        selectedObject = null;

    }

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
                drawTextObjects();

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

        if (lastSelectedTextObject) {

            lastSelectedTextObject.scale = newSize / lastSelectedTextObject.fontSize;
            drawTextObjects();

        }
    })

    addTextBtn.addEventListener("click", createTextBox);
    addImageBtn.addEventListener("click", () => imageLoader.click());
    imageLoader.addEventListener("change", loadImage);

    // PC 마우스 이벤트
    objectCanvas.addEventListener("mousedown", startDragOrDrawing);
    objectCanvas.addEventListener("mousemove", dragOrDraw);
    objectCanvas.addEventListener("mouseup", stopDragOrDrawing);
    objectCanvas.addEventListener("mouseleave", stopDragOrDrawing);

    // 모바일 터치 이벤트
    objectCanvas.addEventListener("touchstart", startDragOrDrawing);
    objectCanvas.addEventListener("touchmove", dragOrDraw);
    objectCanvas.addEventListener("touchend", stopDragOrDrawing);

    // --- 초기 실행 ---
    initializeCanvases();

}