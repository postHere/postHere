function createTextBox(objects, setLastSelectedTextObject, textInput, fontSizeContainer, fontSizeSlider, drawTextObjects, selectedColor, objectCtx, rect) {

    const text = textInput.value;
    if (!text) return;

    const textBox = {
        type: 'text',
        text: text, // 캔버스 내 텍스트 객체 좌측 끝 좌표. 캔버스 축을 이동하는 translate와는 별개.
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
    setLastSelectedTextObject(textBox);
    textInput.value = "";
    fontSizeContainer.style.display = "block";
    drawTextObjects(objects, objectCtx, rect);
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

function getEventCoordinates(event, rect) {

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

function drawTextObjects(objects, objectCtx, rect) {
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

export {createTextBox, getDistance, getAngle, getEventCoordinates, drawTextObjects};
