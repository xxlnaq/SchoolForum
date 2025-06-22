import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import axios from "axios";
import {createPinia} from "pinia";

import 'element-plus/theme-chalk/dark/css-vars.css'
import '@/assets/quill.css'

axios.defaults.baseURL = 'http://localhost:8080'

const app = createApp(App)
const  pinia=createPinia()
app.use(pinia)

app.use(router)

app.mount('#app')
