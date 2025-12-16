import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import path from 'node:path'
import fs from 'node:fs'

export default defineConfig({
	plugins: [react()],
	server: {
		host: true,
		port: 3000,
		strictPort: true
	},
	resolve: {
		alias: {
			'@auth': path.resolve(__dirname, 'src/auth'),
			'@components': path.resolve(__dirname, 'src/components'),
			'@utils': path.resolve(__dirname, 'src/utils'),
			'@types': path.resolve(__dirname, 'src/types'),
			'@services': path.resolve(__dirname, 'src/services'),
			'@pages': path.resolve(__dirname, 'src/pages')
		}
	}
})


