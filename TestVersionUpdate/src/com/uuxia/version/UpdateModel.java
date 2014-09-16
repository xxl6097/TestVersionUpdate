package com.uuxia.version;

import java.io.Serializable;
import java.util.List;

public class UpdateModel implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 3032633771440660672L;
	private int verCode;
	private String verName;
	private int force;
	private List<Info> content;
	private String downloadUrl;
	public int getVerCode() {
		return verCode;
	}
	public void setVerCode(int verCode) {
		this.verCode = verCode;
	}
	public String getVerName() {
		return verName;
	}
	public void setVerName(String verName) {
		this.verName = verName;
	}
	public List<Info> getContent() {
		return content;
	}
	public void setContent(List<Info> content) {
		this.content = content;
	}
	public String getDownloadUrl() {
		return downloadUrl;
	}
	public void setDownloadUrl(String downloadUrl) {
		this.downloadUrl = downloadUrl;
	}
	public UpdateModel() {
		super();
	}
	
	public int getForce() {
		return force;
	}
	public void setForce(int force) {
		this.force = force;
	}

	static class Info implements Serializable{
		/**
		 * 
		 */
		private static final long serialVersionUID = 2447414603933382572L;
		private int id;
		private String text;
		public int getId() {
			return id;
		}
		public void setId(int id) {
			this.id = id;
		}
		public String getText() {
			return text;
		}
		public void setText(String text) {
			this.text = text;
		}
		@Override
		public String toString() {
			return "Info [id=" + id + ", text=" + text + "]";
		}
		public Info(int id, String text) {
			super();
			this.id = id;
			this.text = text;
		}
		public Info() {
			super();
		}
		
	}
}
