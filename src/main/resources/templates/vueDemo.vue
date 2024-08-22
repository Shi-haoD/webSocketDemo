<template>
	<div></div>
</template>
<script>
export default {
	name: '',
	components: {},
	mixins: [],
	props: {},
	data() {
		return {
			socket: null,
		};
	},
	computed: {},
	watch: {},
	mounted() {
		this.getSocket();
		//调用 发送消息
		this.sendMessage('退出登录', '');
	},
	methods: {
		// 初始化WebSocket连接
		getSocket() {
			console.log('尝试连接 WebSocket');
			if (typeof WebSocket === 'undefined') {
				console.log('您的浏览器不支持WebSocket');
				return;
			}

			console.log('您的浏览器支持WebSocket');
			let socketUrl =
				'http://websocketDemo.drcnet.com.cn/socket/imserver/' +
				this.murmurs;
			socketUrl = socketUrl.replace('https', 'ws').replace('http', 'ws');

			console.log('WebSocket URL:', socketUrl);
			this.socket = new WebSocket(socketUrl);

			// 打开事件
			this.socket.onopen = () => {
				console.log('WebSocket连接已打开');
				this.startHeartbeat(); // 开启心跳机制
			};

			// 收到消息事件
			this.socket.onmessage = (msg) => {
				console.log('收到消息:', msg.data);
				if (
					msg.data === '连接成功' ||
					msg.data === 'heartbeat' ||
					typeof msg.data === 'string'
				) {
					console.log(msg.data);
					return;
				}

				let jsonMsg = {};
				try {
					jsonMsg = JSON.parse(msg.data);
				} catch (e) {
					console.error('解析消息失败:', e);
					return;
				}
			};

			// 关闭事件
			this.socket.onclose = () => {
				console.log('WebSocket连接已关闭');
				this.stopHeartbeat(); // 停止心跳机制
			};

			// 错误事件
			this.socket.onerror = (error) => {
				console.log('WebSocket发生了错误:', error);
			};
		},

		// 向服务器发送消息
		sendMessage(content, userName) {
			if (this.socket && this.socket.readyState === WebSocket.OPEN) {
				const message = JSON.stringify({
					toUserId: 'toUserId',
					contentText: 'contentText',
					userName: 'userName',
				});
				console.log('发送消息:', message);
				this.socket.send(message);
			} else {
				console.log('WebSocket连接未打开，无法发送消息');
			}
		},

		// 开启心跳机制
		startHeartbeat() {
			if (this.socket) {
				console.log('启动心跳机制');
				this.heartbeatInterval = setInterval(() => {
					if (this.socket.readyState === WebSocket.OPEN) {
						let hertmessage = JSON.stringify({
							type: 'heartbeat',
						});
						this.socket.send(hertmessage);
					}
				}, 30000); // 每30秒发送一次心跳包
			}
		},

		// 停止心跳机制
		stopHeartbeat() {
			if (this.heartbeatInterval) {
				clearInterval(this.heartbeatInterval);
				this.heartbeatInterval = null;
				console.log('停止心跳机制');
			}
		},
	},
};
</script>
<style lang="" scoped></style>
