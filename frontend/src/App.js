import { BrowserRouter as Router, Routes, Route, Link } from 'react-router-dom';
import FormularPlata from './components/FormularPlata';
// ✅ Importul folosește calea RaportAnaliza, care se potrivește cu fișierul RaportAnaliza.jsx
import RaportAnaliza from './components/RaportAnaliza'; 

function App() {
 return (
   <Router>
     {/* Opțional: o navigație simplă */}
     <nav style={{ background: '#222', padding: '10px', textAlign: 'center' }}>
       <Link to="/" style={{ color: '#d9a441', margin: '0 15px' }}>Plată Parcare</Link>
      <Link to="/raport" style={{ color: '#d9a441', margin: '0 15px' }}>Raport Analiză</Link>
     </nav>
     
     {/* Rutele aplicației */}
     <Routes>
     <Route path="/" element={<FormularPlata />} />
      {/* Adaugă ruta pentru noul raport */}
      <Route path="/raport" element={<RaportAnaliza />} /> 
     </Routes>
   </Router>
 );
}

export default App;