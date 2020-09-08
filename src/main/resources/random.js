function mulberry32() {
  var t = window.seed += 0x6D2B79F5;
  t = (t ^ t >>> 15) * (t | 1) | 0;
  t ^= t + (t ^ t >>> 7) * (t | 61) | 0;
  return ((t ^ t >>> 14) >>> 0) / 4294967296;
}

window.RandExp.prototype.randInt = function (a, b) {
  return a + Math.floor(mulberry32() * (1 + b - a));
}