import { getDistance, getAngle, getEventCoordinates } from './common-find_park.js';

/**
 * 캔버스 객체 상호작용을 관리하는 클래스
 */
export class CanvasInteractionManager {
    constructor(canvas, objects, config) {
        this.canvas = canvas;
        this.rect = canvas.getBoundingClientRect();
        this.objects = objects;
        this.config = config; // { findSelectableObject, onObjectSelect, onObjectMove, onDrawStart, onDrawMove, onInteractionEnd, onPinchUpdate }

        // 내부 상태 변수
        this.isDragging = false;
        this.isPinching = false;
        this.isDrawing = false;
        this.selectedObject = null;

        this.isPotentialDrag = false; // '클릭'일지 '드래그'일지 모르는 상태
        this.DRAG_THRESHOLD = 5; // 드래그로 인식할 최소 픽셀 이동 거리
        this.startX = 0; // 터치 시작 X 좌표
        this.startY = 0; // 터치 시작 Y 좌표

        this.lastX = 0;
        this.lastY = 0;
        this.dragOffsetX = 0;
        this.dragOffsetY = 0;
        this.initialPinchDistance = null;
        this.pinchStartState = {};

        // 이벤트 핸들러의 'this'를 클래스 인스턴스에 바인딩
        this.handleStart = this.handleStart.bind(this);
        this.handleMove = this.handleMove.bind(this);
        this.handleEnd = this.handleEnd.bind(this);
    }

    /**
     * 마우스 및 터치 이벤트를 캔버스에 등록합니다.
     */
    registerEvents() {
        this.canvas.addEventListener("mousedown", this.handleStart);
        this.canvas.addEventListener("mousemove", this.handleMove);
        this.canvas.addEventListener("mouseup", this.handleEnd);
        this.canvas.addEventListener("mouseleave", this.handleEnd);

        this.canvas.addEventListener("touchstart", this.handleStart);
        this.canvas.addEventListener("touchmove", this.handleMove);
        this.canvas.addEventListener("touchend", this.handleEnd);
    }

    handleStart(event) {

        if (event.type === 'touchstart') {
            event.preventDefault();
        }

        if (this.config.isInteractionDisabled && this.config.isInteractionDisabled()) return;

        // 두 손가락 터치 (핀치 줌/회전)
        if (event.touches && event.touches.length === 2) {
            const touch1 = event.touches[0];
            const touch2 = event.touches[1];

            // 1. 각 손가락의 캔버스 내 좌표를 계산합니다.
            const coords1 = { x: touch1.clientX - this.rect.left, y: touch1.clientY - this.rect.top };
            const coords2 = { x: touch2.clientX - this.rect.left, y: touch2.clientY - this.rect.top };

            // 2. 각 좌표에 있는 객체를 찾습니다.
            const object1 = this.config.findSelectableObject(coords1);
            const object2 = this.config.findSelectableObject(coords2);

            // 3. 두 손가락이 모두 존재하고 동일한 객체를 가리키는지 확인합니다.
            if (object1 && object1 === object2) {
                this.selectedObject = object1;
            } else {
                this.selectedObject = null; // 다르거나 객체가 없으면 null로 초기화
            }

            // 4. (기존 로직 유지) 특정 객체를 선택하지 못했다면, 배경 이미지를 제어 대상으로 설정합니다.
            if (!this.selectedObject && this.config.getBackgroundImage) {
                this.selectedObject = this.config.getBackgroundImage();
            }

            // 5. 최종적으로 선택된 객체가 없으면 아무 동작도 하지 않습니다.
            if (!this.selectedObject) return;

            // --- 이하 핀치 줌 준비 코드는 동일합니다. ---
            const pinchCenterX = (touch1.clientX + touch2.clientX) / 2 - this.rect.left;
            const pinchCenterY = (touch1.clientY + touch2.clientY) / 2 - this.rect.top;

            this.isPinching = true;
            this.isDragging = false;
            this.isDrawing = false;
            this.initialPinchDistance = getDistance(touch1, touch2);

            this.pinchStartState = {
                translateX: this.selectedObject.translateX,
                translateY: this.selectedObject.translateY,
                scale: this.selectedObject.scale,
                rotation: this.selectedObject.rotation,
                pinchCenterX: pinchCenterX,
                pinchCenterY: pinchCenterY,
                initialAngle: getAngle(touch1, touch2),
            };

            return;
        }

        // 한 손가락 터치 또는 마우스 클릭
        const { offsetX, offsetY } = getEventCoordinates(event, this.rect);
        this.selectedObject = this.config.findSelectableObject({ x: offsetX, y: offsetY });

        if (this.selectedObject) {
            // 객체를 선택한 경우: 드래그 '준비' 상태로 변경
            this.isPotentialDrag = true;
            this.isDragging = false; // ✨ 드래그 상태를 명확하게 초기화
            this.startX = offsetX;
            this.startY = offsetY;
            this.dragOffsetX = offsetX - this.selectedObject.translateX;
            this.dragOffsetY = offsetY - this.selectedObject.translateY;
        } else if (this.config.onDrawStart) {
            // 객체를 선택하지 않은 경우: 그리기 모드 시작
            this.isDrawing = true;
            [this.lastX, this.lastY] = [offsetX, offsetY];
            this.config.onDrawStart?.();
        }
    }

    handleMove(event) {

        if (event.type === 'touchmove') {
            event.preventDefault();
        }

        // 핀치 줌/회전 중
        if (this.isPinching && event.touches && event.touches.length === 2) {
            if (!this.selectedObject) return;
            const touch1 = event.touches[0];
            const touch2 = event.touches[1];

            // 스케일 계산
            const newDistance = getDistance(touch1, touch2);
            const scaleFactor = newDistance / this.initialPinchDistance;

            // 회전 계산
            const currentAngle = getAngle(touch1, touch2);
            const rotationDelta = currentAngle - this.pinchStartState.initialAngle;

            // 이동(Pan) 계산
            const currentPinchCenterX = (touch1.clientX + touch2.clientX) / 2 - this.rect.left;
            const currentPinchCenterY = (touch1.clientY + touch2.clientY) / 2 - this.rect.top;
            const deltaX = currentPinchCenterX - this.pinchStartState.pinchCenterX;
            const deltaY = currentPinchCenterY - this.pinchStartState.pinchCenterY;

            this.selectedObject.scale = this.pinchStartState.scale * scaleFactor;
            this.selectedObject.rotation = this.pinchStartState.rotation + rotationDelta;
            this.selectedObject.translateX = this.pinchStartState.translateX + deltaX;
            this.selectedObject.translateY = this.pinchStartState.translateY + deltaY;

            // 핀치 중 UI 업데이트를 위한 콜백 호출
            if (this.config.onPinchUpdate) {
                const updated = this.config.onPinchUpdate(this.selectedObject);
                // 콜백에서 값을 변경했을 수 있으므로, 상태 다시 저장
                if (updated) {
                    this.initialPinchDistance = newDistance;
                    this.pinchStartState.scale = this.selectedObject.scale;
                }
            }

            this.config.onObjectMove?.();
            return;
        }

        const { offsetX, offsetY } = getEventCoordinates(event, this.rect);

        // 드래그 판단 로직
        if (this.isPotentialDrag && this.selectedObject) {
            const dx = offsetX - this.startX;
            const dy = offsetY - this.startY;
            if (Math.sqrt(dx * dx + dy * dy) > this.DRAG_THRESHOLD) {
                this.isPotentialDrag = false; // '준비' 상태 해제
                this.isDragging = true;       // '실제 드래그' 상태로 전환
            }
        }

        // 실제 액션이 수행될 때만 preventDefault를 호출하도록 변경
        if (this.isDragging && this.selectedObject) {
            event.preventDefault(); // ✨ 실제 액션이 있을 때만 preventDefault 호출
            this.selectedObject.translateX = offsetX - this.dragOffsetX;
            this.selectedObject.translateY = offsetY - this.dragOffsetY;
            this.config.onObjectMove?.();
        } else if (this.isDrawing && this.config.onDrawMove) {
            event.preventDefault(); // ✨ 실제 액션이 있을 때만 preventDefault 호출
            this.config.onDrawMove({ x: this.lastX, y: this.lastY }, { x: offsetX, y: offsetY });
            [this.lastX, this.lastY] = [offsetX, offsetY];
        }
    }

    handleEnd() {
        // '드래그', '핀치 줌'이 아니라 '클릭'이었는지 명확하게 판단
        // isPotentialDrag는 true로 시작해서 드래그가 시작되면 false가 됨
        const isClick = this.isPotentialDrag && !this.isDragging && !this.isPinching;

        if (isClick && this.selectedObject) {
            this.config.onObjectSelect?.(this.selectedObject);
        }

        if (this.isPinching) {
            this.pinchStartState = {};
            this.initialPinchDistance = null;
        }

        // 모든 상호작용 상태를 깔끔하게 초기화
        this.isDragging = false;
        this.isPinching = false;
        this.isDrawing = false;
        this.isPotentialDrag = false;
        this.selectedObject = null;

        this.config.onInteractionEnd?.();
    }
}