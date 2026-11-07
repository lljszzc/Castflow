# 听见 / Castflow 图标概念第一版

使用内置 image_gen 生成，供用户选型，尚未替换安装图标。

提示词方向：一张2×2对比板，每个大图标下附小尺寸预览；浅灰背景；主符号清晰、少细节、保留Android自适应图标安全区；半透明磨砂玻璃与渐变、柔和光线，无音符、无多余装饰、无图标内文字。

A 蓝紫：播客人形与广播弧线。
B 青绿：麦克风与扩散声波。
C 珊瑚暖橙：耳机与音频波形。
D 深蓝：播放三角与未闭合广播环。

对比板：castflow-options-v1.png。

## A方向红色3D修订
使用内置image_gen，以第一版对比板A为参考编辑。提示词：保留播客人形与双声波弧线；Apple Music风格红/玫红渐变；图案从右下朝左上；约45度俯视的半透明磨砂玻璃浮雕，显露顶面和侧壁，柔和接触阴影；图标底板保持正向圆角方形；无文字、音符或额外装饰。文件castflow-a-red-3d-v2.png，尚未安装。

## 已采用并接入应用
用户确认采用红色3D玻璃方案。通过内置image_gen导出满幅方形版，提示词为：仅将原图白色圆角外区域延展成相同红色背景，其余符号、方向、光影与尺寸保持。产物castflow-a-red-3d-fullbleed.png；应用资源drawable-nodpi/castflow_icon_art.png。manifest接入ic_launcher自适应图标；圆形图标同资源；API33+补充原生矢量单色主题图标。前景以百分比留白，避免固定dp在不同图标尺寸下裁切主体。

构建验收：lintDebug、assemblePerformance通过；最终outputs/castflow-dev.apk覆盖安装成功。aapt2确认manifest图标资源存在。初次应用信息页看到新图标后调整为百分比边距；最终设备进入Dozing，未将黑屏截图当作图标外观验收。
