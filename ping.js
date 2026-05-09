import http from 'k6/http'; export default () => http.post('http://localhost:8080/api/v1/fraud/evaluate/ping');
