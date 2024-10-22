export default function getUrl() {
    if (process.env.NODE_ENV === 'production') {
        return '';
    }
    return 'http://localhost:8080';
}
