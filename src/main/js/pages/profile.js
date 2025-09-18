export function initProfile() {

    // --- Mock Data (예시 데이터) ---
    const findsData = [
        {
            id: 1,
            imageUrl: 'https://images.unsplash.com/photo-1597362925123-5165345093a3?q=80&w=400&auto=format&fit=crop',
            location: 'Samsung-dong',
            isExpiring: true
        },
        {
            id: 2,
            imageUrl: 'https://images.unsplash.com/photo-1588622146903-04938971f153?q=80&w=400&auto=format&fit=crop',
            location: 'Sungsu-dong',
            isExpiring: false
        },
        {
            id: 3,
            imageUrl: 'https://images.unsplash.com/photo-1519996529649-34b33d7935d2?q=80&w=400&auto=format&fit=crop',
            location: 'Gangnam',
            isExpiring: false
        },
        {
            id: 4,
            imageUrl: 'https://images.unsplash.com/photo-1582287135311-8c4f8d2b9f10?q=80&w=400&auto=format&fit=crop',
            location: 'Hongdae',
            isExpiring: true
        },
    ];
    const forumsData = [
        {
            id: 1,
            imageUrl: 'https://images.unsplash.com/photo-1543364195-077a16c30ff3?q=80&w=400&auto=format&fit=crop',
            location: 'Jamsil'
        },
        {
            id: 2,
            imageUrl: 'https://images.unsplash.com/photo-1585154392221-516752dd132f?q=80&w=400&auto=format&fit=crop',
            location: 'Itaewon'
        },
        {
            id: 3,
            imageUrl: 'https://images.unsplash.com/photo-1621282243983-50e5a5933a39?q=80&w=400&auto=format&fit=crop',
            location: 'Sinsa'
        },
        {
            id: 4,
            imageUrl: 'https://images.unsplash.com/photo-1563237023-b1e970526dcb?q=80&w=400&auto=format&fit=crop',
            location: 'Myeong-dong'
        },
    ];

    // --- UI Element References ---
    const openEditModalBtn = document.getElementById('open-edit-modal-btn');
    const editProfileModal = document.getElementById('edit-profile-modal');
    const cancelEditBtn = document.getElementById('cancel-edit-btn');
    const openPasswordModalBtn = document.getElementById('open-password-modal-btn');
    const nicknameInput = document.getElementById('nickname');
    const checkNicknameBtn = document.getElementById('check-nickname-btn');
    const nicknameFeedback = document.getElementById('nickname-feedback');
    const changePasswordModal = document.getElementById('change-password-modal');
    const newPasswordInput = document.getElementById('new-password');
    const confirmPasswordInput = document.getElementById('confirm-password');
    const passwordFeedback = document.getElementById('password-feedback');
    const updatePasswordBtn = document.getElementById('update-password-btn');
    const cancelPasswordBtn = document.getElementById('cancel-password-btn');
    const tabFind = document.getElementById('tab-find');
    const tabForum = document.getElementById('tab-forum');
    const carousel = document.getElementById('carousel');
    const carouselWrapper = document.getElementById('carousel-wrapper');

    let currentTab = 'find', currentPage = 0;
    const postsPerPage = 2;
    let isNicknameAvailable = false;
    let newPassword = null;

    // --- Modal Logic ---
    function openModal(modal) {
        if (modal) modal.style.display = 'flex';
    }

    function closeModal(modal) {
        if (modal) modal.style.display = 'none';
    }

    if (openEditModalBtn) openEditModalBtn.addEventListener('click', () => openModal(editProfileModal));
    if (cancelEditBtn) cancelEditBtn.addEventListener('click', () => closeModal(editProfileModal));
    if (openPasswordModalBtn) openPasswordModalBtn.addEventListener('click', (e) => {
        e.preventDefault();
        closeModal(editProfileModal);
        openModal(changePasswordModal);
    });
    if (cancelPasswordBtn) cancelPasswordBtn.addEventListener('click', () => {
        closeModal(changePasswordModal);
        openModal(editProfileModal);
    });

    if (checkNicknameBtn) checkNicknameBtn.addEventListener('click', () => {
        const nickname = nicknameInput.value;
        if (!nickname) {
            nicknameFeedback.textContent = '닉네임을 입력해주세요.';
            nicknameFeedback.style.color = 'red';
            isNicknameAvailable = false;
            return;
        }
        // This is a simulation of an API call
        setTimeout(() => {
            if (nickname.toLowerCase() === 'admin') {
                nicknameFeedback.textContent = '닉네임 변경 불가!';
                nicknameFeedback.style.color = 'red';
                isNicknameAvailable = false;
            } else {
                nicknameFeedback.textContent = '닉네임 변경 가능!';
                nicknameFeedback.style.color = 'green';
                isNicknameAvailable = true;
            }
        }, 500);
    });

    function validatePassword() {
        if (newPasswordInput.value && newPasswordInput.value === confirmPasswordInput.value) {
            passwordFeedback.textContent = '비밀번호가 일치합니다.';
            passwordFeedback.style.color = 'green';
            updatePasswordBtn.disabled = false;
        } else {
            passwordFeedback.textContent = '비밀번호가 일치하지 않습니다.';
            passwordFeedback.style.color = 'red';
            updatePasswordBtn.disabled = true;
        }
    }

    if (newPasswordInput) newPasswordInput.addEventListener('input', validatePassword);
    if (confirmPasswordInput) confirmPasswordInput.addEventListener('input', validatePassword);

    const changePasswordForm = document.getElementById('change-password-form');
    if (changePasswordForm) changePasswordForm.addEventListener('submit', (e) => {
        e.preventDefault();
        newPassword = newPasswordInput.value;
        alert('비밀번호가 임시 저장되었습니다. Update 버튼을 눌러 최종 적용하세요.');
        closeModal(changePasswordModal);
        openModal(editProfileModal);
    });

    const editProfileForm = document.getElementById('edit-profile-form');
    if (editProfileForm) editProfileForm.addEventListener('submit', (e) => {
        e.preventDefault();
        let updateMessage = '프로필 업데이트:';
        if (isNicknameAvailable && nicknameInput.value) {
            updateMessage += `\n- 새 닉네임: ${nicknameInput.value}`;
        }
        if (newPassword) {
            updateMessage += `\n- 새 비밀번호 설정 완료`;
        }
        alert(updateMessage);
        closeModal(editProfileModal);
    });

    // --- Carousel and Tab Logic ---
    function renderContent() {
        if (!carousel) return;
        const data = currentTab === 'find' ? findsData : forumsData;
        const totalPages = Math.ceil(data.length / postsPerPage);
        carousel.innerHTML = '';
        if (totalPages === 0) {
            carousel.innerHTML = `<div style="text-align:center;width:100%;color:var(--secondary-text-color);">게시물이 없습니다.</div>`;
            return;
        }
        carousel.style.width = `${totalPages * 100}%`;
        for (let i = 0; i < totalPages; i++) {
            const pageElement = document.createElement('div');
            pageElement.className = 'content-page';
            let pageHTML = '';
            const pageData = data.slice(i * postsPerPage, (i + 1) * postsPerPage);
            pageData.forEach(post => {
                const postLink = currentTab === 'find' ? `/find-detail/${post.id}` : `/forum-detail/${post.id}`;
                const statusIcon = post.isExpiring ? '<div class="post-item__status-icon">⏰</div>' : '';
                pageHTML += `<a href="${postLink}" class="post-item"><img class="post-item__image" src="${post.imageUrl}" alt="Post image">${statusIcon}<p class="post-item__location">📍 ${post.location}</p></a>`;
            });
            pageElement.innerHTML = pageHTML;
            carousel.appendChild(pageElement);
        }
        goToPage(0);
    }

    function goToPage(pageIndex) {
        if (!carousel) return;
        const data = currentTab === 'find' ? findsData : forumsData;
        const totalPages = Math.ceil(data.length / postsPerPage) || 1;
        if (pageIndex < 0) pageIndex = 0;
        if (pageIndex >= totalPages) pageIndex = totalPages - 1;
        currentPage = pageIndex;
        const offset = -currentPage * (100 / totalPages);
        carousel.style.transform = `translateX(${offset}%)`;
    }

    if (tabFind) tabFind.addEventListener('click', () => {
        if (currentTab === 'find') return;
        currentTab = 'find';
        tabFind.classList.add('active');
        tabForum.classList.remove('active');
        renderContent();
    });
    if (tabForum) tabForum.addEventListener('click', () => {
        if (currentTab === 'forum') return;
        currentTab = 'forum';
        tabForum.classList.add('active');
        tabFind.classList.remove('active');
        renderContent();
    });

    let touchStartX = 0;
    if (carouselWrapper) {
        carouselWrapper.addEventListener('touchstart', (e) => {
            touchStartX = e.touches[0].clientX;
        }, {passive: true});
        carouselWrapper.addEventListener('touchend', (e) => {
            const touchEndX = e.changedTouches[0].clientX;
            const swipeDistance = touchEndX - touchStartX;
            if (swipeDistance < -50) goToPage(currentPage + 1);
            else if (swipeDistance > 50) goToPage(currentPage - 1);
        }, {passive: true});
    }

    async function loadMyPark() {
        const guestbookWrapper = document.querySelector('.guestbook-wrapper');
        if (!guestbookWrapper) return;

        // 기존의 p 태그를 찾아서 초기 메시지를 설정합니다.
        const guestbookContent = guestbookWrapper.querySelector('.guestbook__content');

        try {
            // 1. 새로 만든 API('/api/v1/users/me/park')를 호출합니다.
            const response = await fetch('/api/v1/users/me/park');

            // 2. 응답이 성공적이면 (200 OK)
            if (response.ok) {
                const parkData = await response.json();

                // 3. 기존 p 태그를 지우고, 받은 URL로 img 태그를 만들어 삽입합니다.
                const guestbookSection = guestbookWrapper.querySelector('.guestbook');
                guestbookSection.innerHTML = ''; // 내부 내용 초기화

                const img = document.createElement('img');
                img.src = parkData.contentCaptureUrl;
                img.alt = 'My Park Guestbook Image';
                img.style.width = '100%';
                img.style.borderRadius = '12px'; // 예쁜 모서리
                img.style.display = 'block';

                guestbookSection.appendChild(img);

            } else {
                // 4. Park 데이터가 없는 경우 (404 Not Found 등) 메시지를 표시합니다.
                if (guestbookContent) {
                    guestbookContent.textContent = '작성된 Park 방명록이 없습니다.';
                }
            }
        } catch (error) {
            console.error('Error loading Park data:', error);
            if (guestbookContent) {
                guestbookContent.textContent = 'Park를 불러오는 중 오류가 발생했습니다.';
            }
        }
    }

    // --- Initial Render ---
    renderContent();
    loadMyPark();
}

