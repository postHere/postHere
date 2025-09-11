/**
 * Google Maps API 스크립트가 완전히 로드된 후, HTML의 callback=initMap에 의해 자동으로 호출되는 함수입니다.
 */
function initMap() {
    // 1. 지도 초기 설정을 위한 기본 위치 (서울 중심)
    const defaultPosition = { lat: 37.5665, lng: 126.9780 };

    // 2. 지도 객체 생성 (기본 위치로 우선 표시)
    const map = new google.maps.Map(document.getElementById("map"), {
        zoom: 16,
        center: defaultPosition,
        disableDefaultUI: true,
    });

    // 3. 모달 관련 DOM 요소 참조 및 이벤트 핸들러 설정
    const modalOverlay = document.getElementById('marker-modal');
    const modalProfileImage = document.getElementById('modal-profile-image');
    const modalNickname = document.getElementById('modal-nickname');
    const modalFindButton = document.getElementById('modal-find-button');
    const modalCloseButton = document.getElementById('modal-close-button');

    // 모달 닫기 이벤트 핸들러
    modalCloseButton.addEventListener('click', () => modalOverlay.style.display = 'none');
    modalOverlay.addEventListener('click', (e) => {
        if (e.target === modalOverlay) {
            modalOverlay.style.display = 'none';
        }
    });

    // '게시글로 이동' 버튼에 대한 이벤트 리스너를 한 번만 등록합니다.
    modalFindButton.addEventListener('click', function() {
        const findPk = this.dataset.findPk;
        if (findPk) {
            // console.log(`Requesting /find/${findPk}`);
            // alert(`게시글로 이동합니다. (ID: ${findPk})`);
            // 실제 페이지 이동 로직
            window.location.href = `/find/${findPk}`;
        }
    });

    // 4. 실제 현재 위치 가져오기
    if (navigator.geolocation) {
        navigator.geolocation.getCurrentPosition(
            (position) => {
                const userPosition = {
                    lat: position.coords.latitude,
                    lng: position.coords.longitude,
                };

                map.setCenter(userPosition);
                new google.maps.Marker({ position: userPosition, map: map, title: "나의 위치", icon: { path: google.maps.SymbolPath.CIRCLE, scale: 8, fillColor: "#4285F4", fillOpacity: 1, strokeWeight: 2, strokeColor: "white" } });
                const circleOptions = { strokeColor: "#4285F4", strokeOpacity: 0.8, strokeWeight: 1, fillColor: "#4285F4", map: map, center: userPosition };
                new google.maps.Circle({ ...circleOptions, radius: 50, fillOpacity: 0.20, clickable: false });
                new google.maps.Circle({ ...circleOptions, radius: 200, fillOpacity: 0.10, clickable: false });

                // 5. 서버에 위치 정보 보내고 주변 데이터 받기
                fetch('http://localhost:3000/location', {   // 로컬 환경 테스트 시 요청 주소
                // fetch('/location', {     // 서버 연결 시 요청 주소
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(userPosition),
                })
                    .then(response => {
                        if (!response.ok) throw new Error('Network response was not ok');
                        return response.json();
                    })
                    .then(data => {
                        data.find_list.forEach(location => {
                            createMarker(location);
                        });
                    })
                    .catch(error => {
                        console.error('Fetch error:', error);
                        alert('주변 정보를 불러오는 데 실패했습니다.');
                    });
            },
            () => {
                alert("위치 정보를 가져올 수 없습니다. 기본 위치(서울)를 표시합니다.");
            }
        );
    } else {
        alert("이 브라우저는 위치 정보 기능을 지원하지 않습니다.");
    }

    /**
     * location 객체를 받아 조건에 맞는 마커를 생성하고 이벤트를 추가하는 함수
     * @param {object} location - 서버로부터 받은 개별 위치 데이터
     */
    function createMarker(location) {
        let markerIcon;

        // 지역(region)에 따라 다른 이미지 아이콘을 설정합니다.
        switch (location.region) {
            case 1:
                markerIcon = { url: 'marker_region_1.png', scaledSize: new google.maps.Size(35, 35) };
                break;
            case 2:
                markerIcon = { url: 'marker_region_2.png', scaledSize: new google.maps.Size(35, 35) };
                break;
            default:
                markerIcon = null; // 200m 밖 (기본 마커)
                break;
        }

        const marker = new google.maps.Marker({
            position: { lat: location.lat, lng: location.lng },
            map: map,
            title: location.nickname,
            icon: markerIcon
        });

        // 모든 마커에 클릭 이벤트를 추가합니다.
        marker.addListener('click', () => {
            // 모달 내용 채우기
            modalProfileImage.src = location.profile_image_url;
            modalNickname.textContent = location.nickname;

            // 지역에 따라 모달 버튼의 활성/비활성 상태를 제어합니다.
            if (location.region === 1) {
                modalNickname.classList.remove('disabled');
                modalFindButton.classList.remove('disabled');
                modalFindButton.dataset.findPk = location.find_pk;
            } else {
                modalNickname.classList.add('disabled');
                modalFindButton.classList.add('disabled');
                delete modalFindButton.dataset.findPk;
            }

            // 모달 보이기
            modalOverlay.style.display = 'flex';
        });
    }
}