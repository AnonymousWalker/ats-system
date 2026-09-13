import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { InputPage } from './pages/InputPage'
import { ReviewPage } from './pages/ReviewPage'

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<InputPage />} />
        <Route path="/analyses/:id" element={<ReviewPage />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  )
}
