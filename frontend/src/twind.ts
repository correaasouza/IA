import { install, observe } from '@twind/core';
import presetTailwind from '@twind/preset-tailwind';

const tw = install({
  presets: [presetTailwind()],
  preflight: false,
  darkMode: false
});

observe(tw, document.documentElement);
