export function initFindViewer() {
    // 👇 [수정] document.addEventListener('DOMContentLoaded', ...) 래퍼를 제거했습니다.

    const wrapper = document.querySelector('.swipe-wrapper');
    const container = document.getElementById('swipe-container');
    const cards = document.querySelectorAll('.card');
    const totalCards = cards.length;
    if (totalCards === 0) return;

    let currentIndex = parseInt(container.dataset.startIndex) || 0;
    let startX = 0;
    let currentTranslate = 0;
    let isDragging = false;
    let cardWidth = 0;

    function calculateCardWidth() {
        if (cards[0]) {
            cardWidth = wrapper.offsetWidth;
            currentTranslate = -currentIndex * cardWidth;
            container.style.transition = 'none';
            container.style.transform = `translateX(${currentTranslate}px)`;
        }
    }

    calculateCardWidth();
    window.addEventListener('resize', calculateCardWidth);

    function goToSlide(index) {
        currentIndex = index;
        currentTranslate = -currentIndex * cardWidth;
        container.style.transition = 'transform 0.3s ease-out';
        container.style.transform = `translateX(${currentTranslate}px)`;
    }

    // --- 이벤트 리스너들 ---
    container.addEventListener('touchstart', (e) => {
        if (e.target.classList.contains('delete-btn')) return;
        startX = e.touches[0].clientX;
        isDragging = true;
        container.style.transition = 'none';
    });

    container.addEventListener('touchmove', (e) => {
        if (!isDragging) return;
        const currentX = e.touches[0].clientX;
        const diff = currentX - startX;
        container.style.transform = `translateX(${currentTranslate + diff}px)`;
    });

    container.addEventListener('touchend', (e) => {
        if (!isDragging) return;
        isDragging = false;
        const endX = e.changedTouches[0].clientX;
        const diff = endX - startX;

        if (Math.abs(diff) > 50) {
            if (diff < 0 && currentIndex < totalCards - 1) {
                currentIndex++;
            } else if (diff > 0 && currentIndex > 0) {
                currentIndex--;
            }
        }
        goToSlide(currentIndex);
    });

    // [삭제 버튼] 이벤트 리스너 (이벤트 위임)
    container.addEventListener('click', async (event) => {
        if (event.target.classList.contains('delete-btn')) {
            const card = event.target.closest('.card');
            const findId = card.dataset.findId;

            if (confirm('정말로 이 Fin\'d를 삭제하시겠습니까?')) {
                try {
                    const response = await fetch(`/api/v1/find/${findId}`, {
                        method: 'DELETE'
                        // CSRF가 비활성화되어 헤더는 불필요
                    });

                    if (response.ok) {
                        card.style.transition = 'opacity 0.3s ease-out';
                        card.style.opacity = '0';
                        setTimeout(() => {
                            card.remove();
                            calculateCardWidth();
                        }, 300);
                    } else {
                        throw new Error('삭제에 실패했습니다.');
                    }
                } catch (error) {
                    console.error('Delete error:', error);
                    alert('삭제 중 오류가 발생했습니다.');
                }
            }
        }
    });
}
