// 文件: js/utils/apiService.js

/**
 * HTML转义函数，防止XSS攻击。
 * @param {string} unsafe 要转义的字符串。
 * @returns {string} 转义后的字符串。
 */
function escapeHtml(unsafe) {
    if (unsafe === null || typeof unsafe === 'undefined') {
        return '';
    }
    return String(unsafe) // 确保是字符串类型
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");
}

/**
 * 通用的API请求函数，集成了统一的错误处理逻辑。
 * @param {string} url API的完整URL。
 * @param {object} options Fetch API的options对象 (包含 method, headers, body等)。
 * @param {string} featureName 功能模块的名称，用于日志记录。
 * @returns {Promise<object>} 成功时返回解析后的JSON业务数据。
 * @throws {Error} 失败时抛出一个Error对象，其message属性包含格式化后的错误信息，
 * 并可能包含 .errorData (解析后的后端ErrorResponse) 或 .responseText (原始文本响应) 属性。
 */
async function fetchApi(url, options = {}, featureName = "API请求") {
    console.log(`${featureName}: 发起请求到 ${options.method || 'GET'} ${url}`);
    if (options.body) {
        // 仅在调试时或确定内容非敏感时打印请求体
        // console.log(`${featureName}: 请求体 (部分):`, String(options.body).substring(0, 200));
    }

    const response = await fetch(url, options);

    console.log(`${featureName} 响应状态:`, response.status, "OK状态:", response.ok);

    if (!response.ok) {
        let errorData;
        let responseTextForDebug = "";
        try {
            const clonedResponse = response.clone();
            errorData = await response.json();
            console.log(`${featureName} 成功解析后端错误JSON:`, errorData);
        } catch (e) {
            console.warn(`${featureName} 解析后端错误响应为JSON失败:`, e);
            try {
                const textResponse = response.clone(); // 再次克隆原始response以读取文本
                responseTextForDebug = await textResponse.text();
                console.warn(`${featureName} 后端返回的原始文本内容:`, responseTextForDebug);
            } catch (textError) {
                console.error(`${featureName} 读取错误响应的文本内容也失败:`, textError);
            }
            errorData = null;
        }

        let baseMessage = `网络响应异常，状态码: ${response.status}`;
        const customError = new Error(baseMessage);
        if (errorData) {
            customError.errorData = errorData;
        } else if (responseTextForDebug) {
            customError.responseText = responseTextForDebug;
        }
        throw customError;
    }
    return response.json(); // 成功时，解析并返回业务数据
}

/**
 * 将捕获到的错误对象格式化为用户可读的HTML字符串，并更新到指定的结果div中。
 * @param {Error} error 捕获到的Error对象 (应包含message，可能包含errorData或responseText)。
 * @param {string} resultDivId 要显示错误信息的目标div的ID。
 * @param {string} featureName 功能模块的名称，用于日志。
 */
function displayFetchError(error, resultDivId, featureName = "操作") {
    const resultDiv = document.getElementById(resultDivId);
    let displayHtml = `<p class="error-message">${escapeHtml(featureName)}出错：`; // 使用更通用的前缀

    if (error && error.message) {
        displayHtml += `<br/>${escapeHtml(error.message)}`; // error.message 是基础消息
    }

    if (error && error.errorData && error.errorData.details && error.errorData.details.length > 0) {
        displayHtml += "<br/>详情如下:<ul>";
        error.errorData.details.forEach(detail => {
            displayHtml += `<li>${escapeHtml(detail)}</li>`;
        });
        displayHtml += "</ul>";
    } else if (error && error.errorData && error.errorData.message) {
        // 避免重复显示包含状态码的基础消息
        if (!error.errorData.message.includes(`状态码: ${error.errorData.status}`) && error.message !== error.errorData.message) {
            displayHtml += `<br/>服务器消息: ${escapeHtml(error.errorData.message)}`;
        }
    } else if (error && error.responseText) {
        displayHtml += `<br/>服务器原始响应 (部分): ${escapeHtml(error.responseText.substring(0, 200))}`;
    } else if (error && !error.errorData && !error.responseText && error.message) {
        // 如果基础消息已经包含了状态码，这里可能不需要再补充“无法获取更详细错误”
        if (!error.message.includes("网络响应异常，状态码:")) {
            displayHtml += `。无法获取更详细的错误信息。`;
        }
    } else {
        displayHtml += `。无法获取更详细的错误信息。`;
    }
    displayHtml += "</p>";

    if (resultDiv) {
        resultDiv.innerHTML = displayHtml;
    }

    console.error(`${featureName} 查询出错详情 (Error Object):`, error);
    if (error && error.errorData) {
        console.error(`${featureName} 后端返回的原始ErrorData:`, error.errorData);
    }
}

// TODO:如果发现escapeHtml错误就采取挂载在window上
// 如果希望将 escapeHtml 设为全局可访问，或者通过模块导出（如果使用模块系统）
// window.escapeHtml = escapeHtml;
// 如果我们把这个apiService.js在html中先于其他feature js引入，那么其他js可以直接使用这里定义的escapeHtml。