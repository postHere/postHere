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

        if (this.config.isInteractionDisabled && this.config.isInteractionDisabled()) return;

        // 두 손가락 터치 (핀치 줌/회전)
        if (event.touches && event.touches.length === 2) {
            const touch1 = event.touches[0];
            const touch2 = event.touches[1];
            const pinchCenterX = (touch1.clientX + touch2.clientX) / 2 - this.rect.left;
            const pinchCenterY = (touch1.clientY + touch2.clientY) / 2 - this.rect.top;

            // 설정(config)을 통해 선택할 객체를 찾음
            this.selectedObject = this.config.findSelectableObject({ x: pinchCenterX, y: pinchCenterY });

            if (!this.selectedObject) return;

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

            this.config.onObjectSelect?.(this.selectedObject);
            return;
        }

        // 한 손가락 터치 또는 마우스 클릭
        const { offsetX, offsetY } = getEventCoordinates(event, this.rect);
        this.selectedObject = this.config.findSelectableObject({ x: offsetX, y: offsetY });

        // 객체를 선택한 경우: 드래그 모드 시작
        if (this.selectedObject) {
            this.isDragging = true;
            this.dragOffsetX = offsetX - this.selectedObject.translateX;
            this.dragOffsetY = offsetY - this.selectedObject.translateY;
            this.config.onObjectSelect?.(this.selectedObject);
            // 객체를 선택하지 않은 경우: 그리기 모드 시작 (설정에 onDrawStart가 있는 경우)
        } else if (this.config.onDrawStart) {
            this.isDrawing = true;
            [this.lastX, this.lastY] = [offsetX, offsetY];
            this.config.onDrawStart?.();
        }
    }

    handleMove(event) {
        event.preventDefault();

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

        // 객체 드래그 중
        if (this.isDragging && this.selectedObject) {
            this.selectedObject.translateX = offsetX - this.dragOffsetX;
            this.selectedObject.translateY = offsetY - this.dragOffsetY;
            this.config.onObjectMove?.();
            // 그리기 중
        } else if (this.isDrawing && this.config.onDrawMove) {
            this.config.onDrawMove({ x: this.lastX, y: this.lastY }, { x: offsetX, y: offsetY });
            [this.lastX, this.lastY] = [offsetX, offsetY];
        }
    }

    handleEnd() {
        if (this.isPinching) {
            this.pinchStartState = {};
            this.initialPinchDistance = null;
        }

        this.isDragging = false;
        this.isPinching = false;
        this.isDrawing = false;
        this.selectedObject = null;

        this.config.onInteractionEnd?.();
    }
}