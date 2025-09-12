export function authHeaders(base = {}) {
    const headers = {...base};
    const token = document.querySelector('meta[name="_csrf"]')?.content || '';
    const headerName = document.querySelector('meta[name="_csrf_header"]')?.content || 'X-CSRF-TOKEN';
    if (token) headers[headerName] = token;
    return headers;
}

// base64url → Uint8Array (VAPID 공개키 변환용)
export function urlBase64ToUint8Array(base64String) {
    const padding = '='.repeat((4 - (base64String.length % 4)) % 4);
    const base64 = (base64String + padding).replace(/-/g, '+').replace(/_/g, '/');
    const rawData = atob(base64);
    const output = new Uint8Array(rawData.length);
    for (let i = 0; i < rawData.length; ++i) output[i] = rawData.charCodeAt(i);
    return output;
}