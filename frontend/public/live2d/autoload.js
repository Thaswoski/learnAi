(function() {
  var live2d_path = '/live2d/';

  console.log = function() {};
  console.warn = function() {};
  console.info = function() {};

  function loadExternalResource(url, type) {
    return new Promise(function(resolve, reject) {
      var tag;
      if (type === 'css') {
        tag = document.createElement('link');
        tag.rel = 'stylesheet';
        tag.href = url;
      }
      else if (type === 'js') {
        tag = document.createElement('script');
        tag.type = 'module';
        tag.src = url;
      }
      if (tag) {
        tag.onload = function() { resolve(url); };
        tag.onerror = function() { reject(url); };
        document.head.appendChild(tag);
      }
    });
  }

  function removeWatermarks() {
    var all = document.querySelectorAll('a,b,span,div,p');
    for (var i = 0; i < all.length; i++) {
      var el = all[i];
      var txt = el.textContent || '';
      if (txt.indexOf('制作') >= 0 || txt.indexOf('Live2D') >= 0 || 
          txt.indexOf('live2d') >= 0 || txt.indexOf('bilibili') >= 0 || 
          txt.indexOf('Bilibili') >= 0 || txt.indexOf('北檵') >= 0) {
        el.remove();
      }
    }
  }

  if (screen.width < 768) return;

  var OriginalImage = window.Image;
  window.Image = function() {
    var img = new OriginalImage();
    img.crossOrigin = "anonymous";
    return img;
  };
  window.Image.prototype = OriginalImage.prototype;

  Promise.all([
    loadExternalResource(live2d_path + 'waifu.css', 'css'),
    loadExternalResource(live2d_path + 'waifu-tips.js', 'js')
  ]).then(function() {
    initWidget({
      waifuPath: live2d_path + 'waifu-tips.json',
      cubism5Path: 'https://cubism.live2d.com/sdk-web/cubismcore/live2dcubismcore.min.js',
      tools: [],
      logLevel: 'off',
      drag: true,
    });

    // 持续清除水印
    removeWatermarks();
    setTimeout(removeWatermarks, 1000);
    setTimeout(removeWatermarks, 2000);
    setTimeout(removeWatermarks, 3000);
    setTimeout(removeWatermarks, 5000);
    
    // 使用 MutationObserver 监控 DOM 变化
    var observer = new MutationObserver(function(mutations) {
      removeWatermarks();
    });
    observer.observe(document.body, { childList: true, subtree: true });
  });
})();
