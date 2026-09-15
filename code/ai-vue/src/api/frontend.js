import service from '@/utils/request'

export const register = (data) => {
  return service.post('/user/add', data)
}

export const startSession = (data) => {
  return service.post('/psychological-chat/session/start', data)
}

export const getSessionList = (params) => {
  return service.get('/psychological-chat/sessions', { params })
}

export const deleteSession = (sessionId) => {
  return service.delete(`/psychological-chat/sessions/${sessionId}`)
}

export const getSessionDetail = (sessionId) => {
  return service.get(`/psychological-chat/sessions/${sessionId}/messages`)
}

export const getSessionEmotion = (sessionId) => {
  return service.get(`/psychological-chat/session/${sessionId}/emotion`)
}

export const addEmotionDiary = (data) => {
  return service.post('/emotion-diary', data)
}

export const getMyDiaries = () => {
  return service.get('/emotion-diary/mine')
}

export const getDiaryTrend = (days = 7) => {
  return service.get('/emotion-diary/trend', { params: { days } })
}

export const changePassword = (data) => {
  return service.put('/user/password', data)
}

export const getKnowledgeList = (params) => {
  return service.get('/knowledge/article/page', { params })
}

export const getKnowledgeDetail = (articleId) => {
  return service.get(`/knowledge/article/${articleId}`)
}

export const getProfile = () => {
  return service.get('/user/profile')
}

export const updateProfile = (data) => {
  return service.put('/user/profile', data)
}

export const updateApiKey = (data) => {
  return service.put('/user/api-key', data)
}

export const uploadAvatar = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('businessType', 'user_avatar')
  formData.append('businessField', 'avatar')
  return service.post('/file/upload', formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  })
}
