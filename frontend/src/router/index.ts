import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import PhotoUploadView from '../views/PhotoUploadView.vue'
import PhotoReorderView from '../views/PhotoReorderView.vue'
import TitleInputView from '../views/TitleInputView.vue'
import ThemeSelectView from '../views/ThemeSelectView.vue'
import BgmSelectView from '../views/BgmSelectView.vue'
import DurationSelectView from '../views/DurationSelectView.vue'
import PreviewView from '../views/PreviewView.vue'
import VideoGeneratingView from '../views/VideoGeneratingView.vue'
import DownloadView from '../views/DownloadView.vue'

export const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'home', component: HomeView },
    {
      path: '/projects/:projectId/upload',
      name: 'photo-upload',
      component: PhotoUploadView,
      props: true,
    },
    {
      path: '/projects/:projectId/reorder',
      name: 'photo-reorder',
      component: PhotoReorderView,
      props: true,
    },
    {
      path: '/projects/:projectId/title',
      name: 'title-input',
      component: TitleInputView,
      props: true,
    },
    {
      path: '/projects/:projectId/theme',
      name: 'theme-select',
      component: ThemeSelectView,
      props: true,
    },
    {
      path: '/projects/:projectId/bgm',
      name: 'bgm-select',
      component: BgmSelectView,
      props: true,
    },
    {
      path: '/projects/:projectId/duration',
      name: 'duration-select',
      component: DurationSelectView,
      props: true,
    },
    {
      path: '/projects/:projectId/preview',
      name: 'preview',
      component: PreviewView,
      props: true,
    },
    {
      path: '/projects/:projectId/generating',
      name: 'video-generating',
      component: VideoGeneratingView,
      props: true,
    },
    {
      path: '/projects/:projectId/download',
      name: 'download',
      component: DownloadView,
      props: true,
    },
    {
      path: '/dev/projects',
      name: 'dev-projects',
      component: () => import('../dev/DevProjectsView.vue'),
    },
  ],
})
