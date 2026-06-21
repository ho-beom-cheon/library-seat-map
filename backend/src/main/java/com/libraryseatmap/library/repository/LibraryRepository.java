package com.libraryseatmap.library.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.libraryseatmap.library.domain.Library;

public interface LibraryRepository extends JpaRepository<Library, String> {

	List<Library> findByDistrict(String district);

	List<Library> findBySourceStdgCd(String sourceStdgCd);
}
