import { BrowserRouter as Router, Routes, Route, Link } from 'react-router-dom';
import FormularPlata from './components/FormularPlata';

import RaportAnaliza from './components/RaportAnaliza'; 

function App() {
 return (
   <Router>
     {}
     <nav style={{ background: '#222', padding: '10px', textAlign: 'center' }}>
       <Link to="/" style={{ color: '#d9a441', margin: '0 15px' }}>Plată Parcare</Link>
      <Link to="/raport" style={{ color: '#d9a441', margin: '0 15px' }}>Raport Analiză</Link>
     </nav>
     
     {}
     <Routes>
     <Route path="/" element={<FormularPlata />} />
      {}
      <Route path="/raport" element={<RaportAnaliza />} /> 
     </Routes>
   </Router>
 );
}

export default App;
