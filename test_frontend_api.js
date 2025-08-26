// Script à exécuter dans la console du navigateur
console.log('🧪 Test Frontend API...');

// Test 1: Vérifier que fetch fonctionne
fetch('/api/planning/view-simple')
  .then(response => {
    console.log('📊 Response status:', response.status);
    console.log('📊 Response headers:', [...response.headers.entries()]);
    return response.json();
  })
  .then(data => {
    console.log('✅ Data received:', data.length, 'plannings');
    console.log('📋 Sample data:', data[0]);
  })
  .catch(error => {
    console.error('❌ Fetch error:', error);
  });

// Test 2: Vérifier l'import du service API
try {
  console.log('🔍 Checking API service...');
  // Cette ligne dépend de votre structure d'import
  console.log('API service should be available in the app');
} catch (error) {
  console.error('❌ API service error:', error);
}
