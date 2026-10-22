/* Stable boundary for a future authenticated sharing adapter. No network calls in v1. */
window.SharingPort=Object.freeze({enabled:false,async pull(spaceId,cursor){throw Error('共享功能尚未启用')},async push(spaceId,changes){throw Error('共享功能尚未启用')},async invite(spaceId,member){throw Error('共享功能尚未启用')}});
