const esbuild = require('esbuild');
const minify = require('html-minifier').minify;
const fs = require('fs');
const path = require('path');
const globSync = require('glob').sync;
const crypto = require('crypto');

const isProd = process.env.NODE_ENV === 'production';

// 从命令行获取目标目录，默认为当前目录下的资源路径
const baseDir = (process.argv[2] || 'build/resources/main').replace(/\\/g, '/');
if (!baseDir) {
    console.error('错误: 未指定目标处理目录。使用方法: node build.js <target_dir>');
    process.exit(1);
}

// 安全检查：绝对禁止处理源码目录
if (baseDir.includes('src/main/resources') || baseDir.includes('src\\main\\resources')) {
    console.error('!!! 安全警告 !!!');
    console.error('脚本检测到目标路径指向源码目录 (src/main/resources)。');
    console.error('构建脚本仅允许处理 build 输出目录，以防止源码被破坏。');
    process.exit(1);
}

const paths = {
    staticSrc: `${baseDir}/static`,
    templatesSrc: `${baseDir}/templates`,
};

// 存储文件指纹映射
const fileHashes = {};

function getHash(content) {
    // 取 MD5 哈希的前 8 位，兼顾唯一性与简洁性
    return crypto.createHash('md5').update(content).digest('hex').substring(0, 8);
}

async function build() {
    console.log(`正在处理目录: ${baseDir}`);
    
    // 0. 清理旧的指纹文件，防止重复处理
    console.log('正在清理旧的指纹文件...');
    const oldHashedFiles = [
        ...globSync(`${paths.staticSrc}/js/**/*-[a-f0-9]{8}.js`),
        ...globSync(`${paths.staticSrc}/css/**/*-[a-f0-9]{8}.css`)
    ];
    oldHashedFiles.forEach(file => {
        try {
            fs.unlinkSync(file);
        } catch (e) {
            console.warn(`清理文件失败: ${file}`, e);
        }
    });

    console.log('开始构建静态资源...');

    try {
        // 1. 处理 JS
        const jsFiles = globSync(`${paths.staticSrc}/js/**/*.js`, { 
            ignore: [
                `${paths.staticSrc}/js/**/*.min.js`,
                `${paths.staticSrc}/js/**/*-[a-f0-9]{8}.js`
            ] 
        });
        console.log(`正在压缩 ${jsFiles.length} 个 JS 文件...`);
        for (const file of jsFiles) {
            const content = fs.readFileSync(file, 'utf8');
            const result = await esbuild.transform(content, {
                minify: true,
                minifyIdentifiers: true, // 混淆变量名
                minifySyntax: true,      // 优化语法
                minifyWhitespace: true,  // 移除空格
                target: 'es2015',
                loader: 'js'
            });
            fs.writeFileSync(file, result.code);
            
            // 记录指纹
            const relPath = '/' + path.relative(paths.staticSrc, file).replace(/\\/g, '/');
            fileHashes[relPath] = getHash(result.code);
        }

        // 2. 处理 CSS
        const cssFiles = globSync(`${paths.staticSrc}/css/**/*.css`, { 
            ignore: [
                `${paths.staticSrc}/css/**/*.min.css`,
                `${paths.staticSrc}/css/**/*-[a-f0-9]{8}.css`
            ] 
        });
        console.log(`正在压缩 ${cssFiles.length} 个 CSS 文件...`);
        for (const file of cssFiles) {
            const content = fs.readFileSync(file, 'utf8');
            const result = await esbuild.transform(content, {
                minify: true,
                loader: 'css',
                legalComments: 'none' // 移除版权注释
            });
            fs.writeFileSync(file, result.code);

            // 记录指纹
            const relPath = '/' + path.relative(paths.staticSrc, file).replace(/\\/g, '/');
            fileHashes[relPath] = getHash(result.code);
        }

        // 3. 全局资源指纹替换 (针对 HTML, JS, CSS 中的所有引用)
        const allFiles = [
            ...globSync(`${paths.staticSrc}/**/*.html`),
            ...globSync(`${paths.templatesSrc}/**/*.html`),
            ...jsFiles,
            ...cssFiles
        ];

        console.log(`正在为 ${allFiles.length} 个文件中的资源引用添加指纹...`);
        for (const file of allFiles) {
            let content = fs.readFileSync(file, 'utf8');
            let processed = content;
            const isHtml = file.endsWith('.html');
            const isSitemap = file.endsWith('sitemap_xml.html');
            const isJs = file.endsWith('.js');
            const isCss = file.endsWith('.css');

            // 3.1 如果是 HTML，进行压缩（排除 sitemap_xml.html）
            if (isHtml && !isSitemap) {
                processed = minify(processed, {
                    collapseWhitespace: true,
                    removeComments: true,
                    minifyJS: true,
                    minifyCSS: true,
                    removeAttributeQuotes: false,
                    removeRedundantAttributes: true,
                    removeScriptTypeAttributes: true,
                    removeStyleLinkTypeAttributes: true,
                    useShortDoctype: true,
                    ignoreCustomFragments: [
                        /\[\[[\s\S]*?\]\]/,
                        /\[\([\s\S]*?\)\]/,
                    ],
                    caseSensitive: true,
                    keepClosingSlash: true,
                });
            }

            // 3.2 资源指纹替换
            let modified = false;
            for (const [relPath, hash] of Object.entries(fileHashes)) {
                const ext = path.extname(relPath);
                const baseName = relPath.substring(0, relPath.length - ext.length);
                const versionedPath = `${baseName}-${hash}${ext}`;
                
                const escapedBaseName = baseName.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
                const escapedExt = ext.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
                // 灵活匹配：支持原始路径或已带有 8 位指纹的路径
                const flexiblePath = `${escapedBaseName}(-[a-f0-9]{8})?${escapedExt}`;

                if (isHtml) {
                    // HTML: 匹配 src=/js/xxx.js 或 href=/css/xxx.css 或 th:src=@{/js/xxx.js} 或 th:href=@{/css/xxx.css}
                    // 1. 匹配标准属性
                    const standardRegex = new RegExp(`(src|href)=(['"]?)${flexiblePath}\\2`, 'g');
                    if (standardRegex.test(processed)) {
                        processed = processed.replace(standardRegex, (match, p1, p2) => `${p1}=${p2}${versionedPath}${p2}`);
                        modified = true;
                    }
                    // 2. 匹配 Thymeleaf 属性
                    const thymeleafRegex = new RegExp(`(th:src|th:href)=(['"]?)@\\{${flexiblePath}\\}\\2`, 'g');
                    if (thymeleafRegex.test(processed)) {
                        processed = processed.replace(thymeleafRegex, (match, p1, p2) => `${p1}=${p2}@{${versionedPath}}${p2}`);
                        modified = true;
                    }
                } else if (isJs) {
                    // JS: 匹配引号包裹的路径
                    const regex = new RegExp(`(['"])${flexiblePath}\\1`, 'g');
                    if (regex.test(processed)) {
                        processed = processed.replace(regex, (match, p1) => `${p1}${versionedPath}${p1}`);
                        modified = true;
                    }
                } else if (isCss) {
                    // CSS: 匹配 url(/path/to/res) 或引号包裹
                    const regex = new RegExp(`(url\\(['"]?|['"])${flexiblePath}(['"]?\\)?)`, 'g');
                    if (regex.test(processed)) {
                        processed = processed.replace(regex, (match, p1, p2) => {
                            if (match.startsWith('url')) return match.replace(new RegExp(flexiblePath), versionedPath);
                            return `${p1}${versionedPath}${p1}`;
                        });
                        modified = true;
                    }
                }
            }

            // 只有内容发生变化或原本是 HTML（因为 HTML 肯定被压缩了）才写入
            if (modified || (isHtml && processed !== content)) {
                fs.writeFileSync(file, processed);
            }
        }

        console.log('静态资源构建完成！指纹库大小:', Object.keys(fileHashes).length);

        // 4. 物理重命名/复制文件 (生成带指纹的实体文件)
        console.log('正在生成带指纹的实体文件...');
        for (const [relPath, hash] of Object.entries(fileHashes)) {
            const ext = path.extname(relPath);
            const baseName = relPath.substring(0, relPath.length - ext.length);
            const versionedPath = `${baseName}-${hash}${ext}`;
            
            const sourceFile = path.join(paths.staticSrc, relPath);
            const targetFile = path.join(paths.staticSrc, versionedPath);
            
            if (fs.existsSync(sourceFile)) {
                fs.renameSync(sourceFile, targetFile);
                // console.log(`  - 重命名: ${relPath} -> ${versionedPath}`);
            }
        }
        console.log('带指纹的实体文件生成完毕！');
    } catch (error) {
        console.error('构建失败:', error);
        process.exit(1);
    }
}

build();
