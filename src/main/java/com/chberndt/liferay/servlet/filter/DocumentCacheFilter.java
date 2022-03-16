package com.chberndt.liferay.servlet.filter;

import com.liferay.document.library.kernel.service.DLAppLocalServiceUtil;
import com.liferay.petra.string.CharPool;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.repository.model.FileEntry;
import com.liferay.portal.kernel.repository.model.FileShortcut;
import com.liferay.portal.kernel.security.permission.ActionKeys;
import com.liferay.portal.kernel.security.permission.PermissionChecker;
import com.liferay.portal.kernel.security.permission.PermissionCheckerFactoryUtil;
import com.liferay.portal.kernel.security.permission.PermissionThreadLocal;
import com.liferay.portal.kernel.service.UserLocalServiceUtil;
import com.liferay.portal.kernel.servlet.BaseFilter;
import com.liferay.portal.kernel.servlet.HttpHeaders;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.HttpUtil;
import com.liferay.portal.kernel.util.PortalUtil;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.service.component.annotations.Component;

/**
 * @author Christian Berndt
 */
@Component(
		property = {
			// TODO: before / after which filter? Does it matter?
			"before-filter=Auto Login Filter", 
			"servlet-context-name=",
			"servlet-filter-name=Document Caching Filter",
			"url-pattern=/documents/*",
			// TODO: where is "/image" used?
			"url-pattern=/image/*",
			"url-pattern=/o/adaptive-media/*"
		},
		service = Filter.class
	)
public class DocumentCacheFilter extends BaseFilter {

 @Override
protected void processFilter(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse,
		FilterChain filterChain) throws Exception {

		System.out.println("DocumentCacheFilter.processFilter()");

		// HttpServletRequest httpServletRequest = (HttpServletRequest)request;
		
		httpServletRequest.getRequestURI();
		
		System.out.println("httpServletRequest.getRequestURI() = " + httpServletRequest.getRequestURI());
		
		User defaultUser = UserLocalServiceUtil.getDefaultUser(PortalUtil.getCompanyId(httpServletRequest));
		System.out.println("defaultUser = " + defaultUser);

		PermissionChecker permissionChecker = PermissionCheckerFactoryUtil.create(defaultUser);
		PermissionThreadLocal.setPermissionChecker(permissionChecker);
		
		if (_hasGuestViewPermission(httpServletRequest, httpServletResponse)) {
			 httpServletResponse.setHeader(HttpHeaders.CACHE_CONTROL, HttpHeaders.CACHE_CONTROL_DEFAULT_VALUE);
			 // TODO: Read max-age from configuration
			 // TODO: Add must-revalidate (see: https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Cache-Control)
		}
		
		filterChain.doFilter(httpServletRequest, httpServletResponse);
		
	}
 
 	/*
 	 * From: import com.liferay.portal.kernel.webserver.WebServerServlet
 	 */
	protected FileEntry getFileEntry(String[] pathArray) throws Exception {
		
		if (pathArray.length == 1) {
			long fileShortcutId = GetterUtil.getLong(pathArray[0]);

			FileShortcut dlFileShortcut = DLAppLocalServiceUtil.getFileShortcut(
					fileShortcutId);
//			FileShortcut dlFileShortcut = DLAppServiceUtil.getFileShortcut(
//					fileShortcutId);

			return DLAppLocalServiceUtil.getFileEntry(
				dlFileShortcut.getToFileEntryId());
//			return DLAppServiceUtil.getFileEntry(
//			dlFileShortcut.getToFileEntryId());
		}
		else if (pathArray.length == 2) {
			long groupId = GetterUtil.getLong(pathArray[0]);

			return DLAppLocalServiceUtil.getFileEntryByUuidAndGroupId(
					pathArray[1], groupId);
//			return DLAppServiceUtil.getFileEntryByUuidAndGroupId(
//					pathArray[1], groupId);
		}
		else if (pathArray.length == 3) {
			long groupId = GetterUtil.getLong(pathArray[0]);
			long folderId = GetterUtil.getLong(pathArray[1]);

			String fileName = HttpUtil.decodeURL(pathArray[2]);

			if (fileName.contains(StringPool.QUESTION)) {
				fileName = fileName.substring(
					0, fileName.indexOf(StringPool.QUESTION));
			}

			return DLAppLocalServiceUtil.getFileEntry(groupId, folderId, fileName);
//			return DLAppServiceUtil.getFileEntry(groupId, folderId, fileName);
		}
		else {
			long groupId = GetterUtil.getLong(pathArray[0]);

			String uuid = pathArray[3];

			return DLAppLocalServiceUtil.getFileEntryByUuidAndGroupId(uuid, groupId);
//			return DLAppServiceUtil.getFileEntryByUuidAndGroupId(uuid, groupId);
		}
	}
	
	private boolean _hasGuestViewPermission (
			HttpServletRequest httpServletRequest,
			HttpServletResponse httpServletResponse)
		throws Exception {

		String path = HttpUtil.fixPath(httpServletRequest.getPathInfo());

		String[] pathArray = StringUtil.split(path, CharPool.SLASH);
		
		boolean hasGuestViewPermission = false;

		if (Validator.isNumber(pathArray[0])) {

			FileEntry fileEntry = getFileEntry(pathArray);

			PermissionChecker permissionChecker =
				PermissionThreadLocal.getPermissionChecker();
			
			hasGuestViewPermission =  fileEntry.containsPermission(permissionChecker, ActionKeys.VIEW);
			
		}
		
		return hasGuestViewPermission;
	}
 
	@Override
	protected Log getLog() {
		return _log;
	}

	private static final Log _log = LogFactoryUtil.getLog(
		DocumentCacheFilter.class);

}