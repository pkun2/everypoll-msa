import { Routes, Route } from 'react-router-dom'
import Layout from './components/Layout'
import PrivateRoute from './components/PrivateRoute'
import Home from './pages/Home'
import Login from './pages/Login'
import Register from './pages/Register'
import PollDetail from './pages/PollDetail'
import CreatePoll from './pages/CreatePoll'

function App() {
  return (
    <Routes>
      <Route path="/" element={<Layout />}>
        <Route index element={<Home />} />
        <Route path="login" element={<Login />} />
        <Route path="register" element={<Register />} />
        <Route path="polls/:pollId" element={<PollDetail />} />
        <Route
          path="polls/create"
          element={
            <PrivateRoute>
              <CreatePoll />
            </PrivateRoute>
          }
        />
      </Route>
    </Routes>
  )
}

export default App