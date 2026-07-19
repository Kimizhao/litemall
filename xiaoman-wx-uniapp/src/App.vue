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

page {
  --primary-gradient: linear-gradient(135deg, #E07A5F 0%, #D86B4F 100%);
  --secondary-gradient: linear-gradient(135deg, #8B8E4B 0%, #767A3D 100%);
  --dark-gradient: linear-gradient(135deg, #6F4E37 0%, #4A3525 100%);
  --accent-gradient: linear-gradient(135deg, #E07A5F 0%, #C85C43 100%);
  --bg-primary: #FAF6EE;
  --bg-secondary: #ffffff;
  --text-main: #4A3525;
  --text-muted: #A18276;
  --border-light: rgba(230, 213, 184, 0.2);
  --shadow-sm: 0 2px 8px rgba(111, 78, 55, 0.04);
  --shadow-md: 0 10px 25px -5px rgba(111, 78, 55, 0.08), 0 8px 16px -6px rgba(111, 78, 55, 0.04);
  --shadow-lg: 0 20px 35px -10px rgba(74, 53, 37, 0.15);
  --radius-sm: 12rpx;
  --radius-md: 24rpx;
  --radius-lg: 36rpx;

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
  border: 1px solid rgba(230, 213, 184, 0.3);
  box-shadow: var(--shadow-sm);
}

.dark-glass-panel {
  background: rgba(74, 53, 37, 0.85);
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
