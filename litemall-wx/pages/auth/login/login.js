var api = require('../../../config/api.js');
var util = require('../../../utils/util.js');
var user = require('../../../utils/user.js');

var app = getApp();
Page({
  data: {
    showAuthModal: false,
    // Default high-end placeholder avatar matching Litemall shop aesthetics
    avatarUrl: 'https://yanxuan.nosdn.127.net/80841d741d7fa3073e0ae27bf487339f.jpg?imageView&quality=90&thumbnail=64x64',
    nickName: '',
    tempAvatarUrl: '' // Tmp WeChat file path on native OS filesystem
  },
  onLoad: function(options) {
    // Initialization
  },
  onReady: function() {
    // Page ready
  },
  onShow: function() {
    // Page show
  },
  onHide: function() {
    // Page hide
  },
  onUnload: function() {
    // Page close
  },

  // Modal Control Actions
  openAuthModal: function() {
    this.setData({
      showAuthModal: true
    });
  },
  closeAuthModal: function() {
    this.setData({
      showAuthModal: false
    });
  },

  // Avatar Selector Callback
  onChooseAvatar: function(e) {
    const { avatarUrl } = e.detail;
    this.setData({
      avatarUrl: avatarUrl,
      tempAvatarUrl: avatarUrl
    });
  },

  // Nickname Selector Callbacks
  onNicknameBlur: function(e) {
    this.setData({
      nickName: e.detail.value
    });
  },
  onNicknameInput: function(e) {
    this.setData({
      nickName: e.detail.value
    });
  },

  // Form submission and login execution
  handleWxLogin: function() {
    const that = this;
    if (!that.data.nickName || !that.data.nickName.trim()) {
      util.showErrorToast('请填写微信昵称');
      return;
    }

    wx.showLoading({
      title: '正在登录...',
      mask: true
    });

    // Case A: User uploaded a custom WeChat avatar - we must upload it to storage first
    if (that.data.tempAvatarUrl) {
      wx.uploadFile({
        url: api.StorageUpload,
        filePath: that.data.tempAvatarUrl,
        name: 'file',
        header: {
          'X-Litemall-Token': wx.getStorageSync('token')
        },
        success: function(res) {
          try {
            const uploadRes = JSON.parse(res.data);
            if (uploadRes.errno === 0) {
              const realAvatarUrl = uploadRes.data.url;
              that.executeLogin(realAvatarUrl, that.data.nickName);
            } else {
              wx.hideLoading();
              util.showErrorToast('头像同步失败');
            }
          } catch (e) {
            wx.hideLoading();
            util.showErrorToast('头像解析异常');
          }
        },
        fail: function() {
          wx.hideLoading();
          util.showErrorToast('头像上传失败');
        }
      });
    } else {
      // Case B: User did not change avatar - proceed directly with default placeholder
      that.executeLogin(that.data.avatarUrl, that.data.nickName);
    }
  },

  // Performs backend registration and silent session handshakes
  executeLogin: function(avatar, nickname) {
    const that = this;
    const userInfo = {
      nickName: nickname,
      avatarUrl: avatar,
      gender: 0
    };

    user.loginByWeixin(userInfo).then(res => {
      wx.hideLoading();
      that.setData({
        showAuthModal: false
      });
      app.globalData.hasLogin = true;
      wx.showToast({
        title: '登录成功',
        icon: 'success',
        duration: 1000
      });

      // Go back to the preceding screen
      setTimeout(() => {
        wx.navigateBack({
          delta: 1
        });
      }, 1000);
    }).catch((err) => {
      wx.hideLoading();
      app.globalData.hasLogin = false;
      util.showErrorToast('微信登录失败');
    });
  },

  accountLogin: function() {
    wx.navigateTo({
      url: "/pages/auth/accountLogin/accountLogin"
    });
  },

  preventBubble: function() {
    // Helper to catch bubble tap on the bottom sheet
  }
});