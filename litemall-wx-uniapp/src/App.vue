<script>
export default {
  onLaunch: function () {
    console.log('App Launch')
  },
  onShow: function () {
    console.log('App Show')
  },
  onHide: function () {
    console.log('App Hide')
  }
}
</script>

<style>
/* Global Styles & Token System */
@import url('https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;500;600;700&display=swap');

:root {
  --primary-gradient: linear-gradient(135deg, #4f46e5 0%, #3b82f6 100%);
  --secondary-gradient: linear-gradient(135deg, #10b981 0%, #059669 100%);
  --dark-gradient: linear-gradient(135deg, #1e293b 0%, #0f172a 100%);
  --accent-gradient: linear-gradient(135deg, #ec4899 0%, #f43f5e 100%);
  --bg-primary: #f8fafc;
  --bg-secondary: #ffffff;
  --text-main: #0f172a;
  --text-muted: #64748b;
  --border-light: rgba(226, 232, 240, 0.8);
  --shadow-sm: 0 2px 8px rgba(0, 0, 0, 0.04);
  --shadow-md: 0 10px 25px -5px rgba(59, 130, 246, 0.1), 0 8px 16px -6px rgba(59, 130, 246, 0.05);
  --shadow-lg: 0 20px 35px -10px rgba(15, 23, 42, 0.15);
  --radius-sm: 12rpx;
  --radius-md: 24rpx;
  --radius-lg: 36rpx;
}

page {
  background-color: var(--bg-primary);
  font-family: 'Outfit', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
  color: var(--text-main);
  font-size: 28rpx;
  line-height: 1.5;
  box-sizing: border-box;
}

/* Glassmorphism utility */
.glass-panel {
  background: rgba(255, 255, 255, 0.75);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.5);
  box-shadow: var(--shadow-sm);
}

.dark-glass-panel {
  background: rgba(15, 23, 42, 0.85);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  box-shadow: var(--shadow-lg);
}

/* Base button reset and custom */
button {
  border: none;
  background: none;
  padding: 0;
  margin: 0;
  line-height: inherit;
  font-family: inherit;
}

button::after {
  border: none;
}

/* Standard scrollbar styling */
::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}

::-webkit-scrollbar-track {
  background: transparent;
}

::-webkit-scrollbar-thumb {
  background: rgba(100, 116, 139, 0.2);
  border-radius: 10px;
}
</style>
